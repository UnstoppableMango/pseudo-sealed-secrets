package dev.unmango;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.StringReader;
import java.security.PublicKey;
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

        String pem = new String(Base64.getDecoder().decode(keypairSecret.getData().get("tls.crt")));
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            SubjectPublicKeyInfo keyInfo = (SubjectPublicKeyInfo) parser.readObject();
            return new JcaPEMKeyConverter().getPublicKey(keyInfo);
        }
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
