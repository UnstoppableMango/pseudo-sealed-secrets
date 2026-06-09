package dev.unmango;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

public class KeypairBootstrapper {

    static final String SECRET_NAME = "pseudo-sealed-secrets-key";

    private static final Logger log = LoggerFactory.getLogger(KeypairBootstrapper.class);

    private final KubernetesClient client;
    private final KeypairService keypairService;

    public KeypairBootstrapper(KubernetesClient client) {
        this(client, new KeypairService());
    }

    public KeypairBootstrapper(KubernetesClient client, KeypairService keypairService) {
        this.client = client;
        this.keypairService = keypairService;
    }

    public void bootstrap(String namespace) {
        var existing = client.secrets().inNamespace(namespace).withName(SECRET_NAME).get();
        if (existing != null) {
            log.info("Keypair secret already exists in namespace {}, skipping generation", namespace);
            return;
        }

        log.info("Generating keypair for namespace {}", namespace);
        KeyPair keyPair;
        try {
            keyPair = keypairService.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA not available", e);
        }

        var secret = new SecretBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(SECRET_NAME)
                        .withNamespace(namespace)
                        .build())
                .withType("Opaque")
                .addToStringData("tls.key", keypairService.toPrivateKeyPem(keyPair.getPrivate()))
                .addToStringData("tls.crt", keypairService.toPublicKeyPem(keyPair.getPublic()))
                .build();

        client.secrets().inNamespace(namespace).resource(secret).create();
        log.info("Keypair secret created in namespace {}", namespace);
    }
}
