KIND_CLUSTER_NAME ?= pseudo-sealed-secrets
KIND_KUBECONFIG   := .kind/kubeconfig
CRD_DIR           := target/classes/META-INF/fabric8

export KIND_EXPERIMENTAL_PROVIDER = podman

.PHONY: build update check lint format fmt test deploy deploy-crds run start-kind stop-kind

build:
	nix build .#

update:
	nix flake update

check lint:
	nix flake check

format fmt:
	nix fmt

test:
	mvn test

$(CRD_DIR): pom.xml $(shell find src/main/java -name '*.java')
	mvn compile -q

deploy-crds: $(KIND_KUBECONFIG) $(CRD_DIR)
	kubectl apply -f $(CRD_DIR) --kubeconfig=$(KIND_KUBECONFIG)

deploy: deploy-crds

run: deploy-crds
	KUBECONFIG=$(KIND_KUBECONFIG) mvn exec:java -Dexec.mainClass=dev.unmango.Runner

$(KIND_KUBECONFIG): kind-config.yaml
	mkdir -p .kind
	kind create cluster \
	  --name $(KIND_CLUSTER_NAME) \
	  --config kind-config.yaml \
	  --kubeconfig $(KIND_KUBECONFIG)

start-kind: $(KIND_KUBECONFIG)

stop-kind:
	kind delete cluster --name $(KIND_CLUSTER_NAME)
	rm -f $(KIND_KUBECONFIG)
