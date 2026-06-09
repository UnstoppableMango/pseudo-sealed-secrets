package dev.unmango;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SealService {

    private final KubernetesClient client;
    private final String namespace;
    private final EncryptionService encryptionService;

    public SealService(KubernetesClient client, String namespace) {
        this(client, namespace, new EncryptionService());
    }

    SealService(KubernetesClient client, String namespace, EncryptionService encryptionService) {
        this.client = client;
        this.namespace = namespace;
        this.encryptionService = encryptionService;
    }

    public PublicKey loadPublicKey() throws Exception {
        Secret keypairSecret = client.secrets()
                .inNamespace(namespace)
                .withName(KeypairBootstrapper.SECRET_NAME)
                .get();
        if (keypairSecret == null) {
            throw new IllegalStateException("Keypair secret not found in namespace " + namespace);
        }

        String pem = keypairSecret.getData().get("tls.crt");
        byte[] der = Base64.getDecoder().decode(pem);
        // Strip PEM headers if present (keypair is stored as PEM in stringData but read back as base64)
        // The secret stores PEM in stringData which k8s base64-encodes, so we decode and strip headers
        String pemStr = new String(der);
        if (pemStr.startsWith("-----")) {
            String stripped = pemStr
                    .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            der = Base64.getDecoder().decode(stripped);
        }

        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    public String getPublicKeyPem() {
        Secret keypairSecret = client.secrets()
                .inNamespace(namespace)
                .withName(KeypairBootstrapper.SECRET_NAME)
                .get();
        if (keypairSecret == null) {
            throw new IllegalStateException("Keypair secret not found in namespace " + namespace);
        }
        String encoded = keypairSecret.getData().get("tls.crt");
        return new String(Base64.getDecoder().decode(encoded));
    }

    public Secret sealSecret(Secret secret) throws Exception {
        PublicKey pubKey = loadPublicKey();
        String secretName = secret.getMetadata().getName();
        String secretNamespace = secret.getMetadata().getNamespace();
        if (secretNamespace == null || secretNamespace.isBlank()) {
            secretNamespace = "default";
        }
        byte[] label = (secretNamespace + "/" + secretName).getBytes();

        Map<String, String> encryptedData = new HashMap<>();

        if (secret.getData() != null) {
            for (Map.Entry<String, String> entry : secret.getData().entrySet()) {
                byte[] plaintext = Base64.getDecoder().decode(entry.getValue());
                byte[] ciphertext = encryptionService.hybridEncrypt(pubKey, plaintext, label);
                encryptedData.put(entry.getKey(), Base64.getEncoder().encodeToString(ciphertext));
            }
        }

        if (secret.getStringData() != null) {
            for (Map.Entry<String, String> entry : secret.getStringData().entrySet()) {
                byte[] plaintext = entry.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] ciphertext = encryptionService.hybridEncrypt(pubKey, plaintext, label);
                encryptedData.put(entry.getKey(), Base64.getEncoder().encodeToString(ciphertext));
            }
        }

        return new SecretBuilder()
                .withMetadata(secret.getMetadata())
                .withType(secret.getType())
                .withData(encryptedData)
                .build();
    }
}
