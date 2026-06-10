package dev.unmango;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@ControllerConfiguration
public class SealedSecretReconciler implements Reconciler<Secret> {

    public static final String TARGET_ANNOTATION = "pseudo-sealed-secrets.unmango.dev/target";

    @Override
    public UpdateControl<Secret> reconcile(Secret resource, Context<Secret> context) throws Exception {
        var annotations = resource.getMetadata().getAnnotations();
        if (annotations == null || !annotations.containsKey(TARGET_ANNOTATION)) {
            return UpdateControl.noUpdate();
        }

        String targetName = annotations.get(TARGET_ANNOTATION);
        String namespace = resource.getMetadata().getNamespace();
        String sealedName = resource.getMetadata().getName();
        byte[] label = (namespace + "/" + sealedName).getBytes(StandardCharsets.UTF_8);

        KubernetesClient client = context.getClient();
        PrivateKey privateKey = SealService.loadPrivateKey(client, namespace);
        EncryptionService encryptionService = new EncryptionService();

        Map<String, String> decryptedData = new HashMap<>();
        if (resource.getData() != null) {
            for (Map.Entry<String, String> entry : resource.getData().entrySet()) {
                byte[] ciphertext = Base64.getDecoder().decode(entry.getValue());
                byte[] plaintext = encryptionService.hybridDecrypt(privateKey, ciphertext, label);
                decryptedData.put(entry.getKey(), new String(plaintext, StandardCharsets.UTF_8));
            }
        }

        Secret target = new SecretBuilder()
                .withNewMetadata()
                    .withName(targetName)
                    .withNamespace(namespace)
                .endMetadata()
                .withStringData(decryptedData)
                .build();

        client.secrets().inNamespace(namespace).resource(target).createOrReplace();

        return UpdateControl.noUpdate();
    }
}
