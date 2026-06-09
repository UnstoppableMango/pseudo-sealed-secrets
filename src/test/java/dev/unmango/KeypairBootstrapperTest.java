package dev.unmango;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static dev.unmango.KeypairBootstrapper.SECRET_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@EnableKubernetesMockClient(crud = true)
class KeypairBootstrapperTest {

    static KubernetesClient client;

    // 2048-bit for test speed; production uses 4096
    private final KeypairBootstrapper bootstrapper =
            new KeypairBootstrapper(client, new KeypairService(2048));

    @Test
    void secretCreatedWithCorrectKeys() {
        bootstrapper.bootstrap("test-ns");

        Secret secret = client.secrets().inNamespace("test-ns").withName(SECRET_NAME).get();
        assertThat(secret).isNotNull();
        assertThat(secret.getType()).isEqualTo("Opaque");

        String privateKey = decodeSecretValue(secret, "tls.key");
        String publicKey = decodeSecretValue(secret, "tls.crt");

        assertThat(privateKey).startsWith("-----BEGIN PRIVATE KEY-----");
        assertThat(privateKey).endsWith("-----END PRIVATE KEY-----\n");
        assertThat(publicKey).startsWith("-----BEGIN PUBLIC KEY-----");
        assertThat(publicKey).endsWith("-----END PUBLIC KEY-----\n");
    }

    @Test
    void bootstrapIsIdempotent() {
        bootstrapper.bootstrap("test-ns");
        String originalKey = decodeSecretValue(
                client.secrets().inNamespace("test-ns").withName(SECRET_NAME).get(), "tls.key");

        bootstrapper.bootstrap("test-ns");
        String keyAfterSecondCall = decodeSecretValue(
                client.secrets().inNamespace("test-ns").withName(SECRET_NAME).get(), "tls.key");

        assertThat(keyAfterSecondCall).isEqualTo(originalKey);
    }

    private String decodeSecretValue(Secret secret, String key) {
        if (secret.getStringData() != null && secret.getStringData().containsKey(key)) {
            return secret.getStringData().get(key);
        }
        if (secret.getData() != null && secret.getData().containsKey(key)) {
            return new String(Base64.getDecoder().decode(secret.getData().get(key)));
        }
        return null;
    }
}
