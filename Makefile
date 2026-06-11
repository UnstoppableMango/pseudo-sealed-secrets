KIND_CLUSTER_NAME ?= pseudo-sealed-secrets
KIND_KUBECONFIG   := .kind/kubeconfig
HELM_RELEASE      ?= pseudo-sealed-secrets

export KIND_EXPERIMENTAL_PROVIDER = podman

.PHONY: build update check lint format fmt test deploy deploy-helm undeploy-helm load-image run start-kind stop-kind

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

load-image: $(KIND_KUBECONFIG)
	nix run .#image.copyToDockerDaemon
	kind load docker-image pseudo-sealed-secrets:latest \
	  --name $(KIND_CLUSTER_NAME)

deploy-helm: load-image
	helm upgrade --install $(HELM_RELEASE) ./helm \
	  --kubeconfig=$(KIND_KUBECONFIG) \
	  --wait

undeploy-helm:
	helm uninstall $(HELM_RELEASE) \
	  --kubeconfig=$(KIND_KUBECONFIG)

deploy: deploy-helm

run: $(KIND_KUBECONFIG)
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
