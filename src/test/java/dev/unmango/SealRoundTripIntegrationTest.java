package dev.unmango;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static dev.unmango.SealedSecretReconciler.TARGET_ANNOTATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SealRoundTripIntegrationTest {

    @RegisterExtension
    LocallyRunOperatorExtension extension = LocallyRunOperatorExtension.builder()
            .withReconciler(SealedSecretReconciler.class)
            .build();

    @BeforeEach
    void bootstrapKeypair() {
        String ns = extension.getNamespace();
        new KeypairBootstrapper(extension.getKubernetesClient(), new KeypairService(2048))
                .bootstrap(ns);
    }

    @Test
    void roundTripsArbitrarySecret() throws Exception {
        String ns = extension.getNamespace();

        SealService sealService = new SealService(extension.getKubernetesClient(), ns);
        Secret original = new SecretBuilder()
                .withNewMetadata()
                    .withName("my-secret-sealed")
                    .withNamespace(ns)
                .endMetadata()
                .withStringData(Map.of("password", "hunter2"))
                .build();
        Secret sealed = sealService.sealSecret(original);

        Secret annotated = new SecretBuilder(sealed)
                .editMetadata()
                    .addToAnnotations(TARGET_ANNOTATION, "my-secret")
                .endMetadata()
                .build();
        extension.create(annotated);

        await().untilAsserted(() -> {
            Secret unsealed = extension.get(Secret.class, "my-secret");
            assertThat(unsealed).isNotNull();
            assertThat(unsealed.getData()).containsKey("password");
            String decoded = new String(Base64.getDecoder().decode(unsealed.getData().get("password")), StandardCharsets.UTF_8);
            assertThat(decoded).isEqualTo("hunter2");
        });
    }
}
