package dev.unmango;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;

public class EncryptionService {

    private static final int SESSION_KEY_SIZE = 32;
    private static final int GCM_NONCE_SIZE = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecureRandom random;

    public EncryptionService() {
        this(new SecureRandom());
    }

    EncryptionService(SecureRandom random) {
        this.random = random;
    }

    /**
     * Hybrid RSA-OAEP + AES-256-GCM encrypt matching sealed-secrets hybridEncrypt.
     * Output format: rsaEncryptedSessionKey | nonce | aesCiphertext
     */
    public byte[] hybridEncrypt(PublicKey pubKey, byte[] plaintext, byte[] label) throws Exception {
        byte[] sessionKey = new byte[SESSION_KEY_SIZE];
        random.nextBytes(sessionKey);

        OAEPParameterSpec oaepSpec = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256,
                new PSource.PSpecified(label));
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, pubKey, oaepSpec);
        byte[] encryptedKey = rsaCipher.doFinal(sessionKey);

        byte[] nonce = new byte[GCM_NONCE_SIZE];
        random.nextBytes(nonce);

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(sessionKey, "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, nonce));
        byte[] aesCiphertext = aesCipher.doFinal(plaintext);

        byte[] result = new byte[encryptedKey.length + nonce.length + aesCiphertext.length];
        System.arraycopy(encryptedKey, 0, result, 0, encryptedKey.length);
        System.arraycopy(nonce, 0, result, encryptedKey.length, nonce.length);
        System.arraycopy(aesCiphertext, 0, result, encryptedKey.length + nonce.length, aesCiphertext.length);
        return result;
    }
}
