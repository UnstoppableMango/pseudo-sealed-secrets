package dev.unmango;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class KeypairService {

    private final int keySize;

    public KeypairService() {
        this(4096);
    }

    KeypairService(int keySize) {
        this.keySize = keySize;
    }

    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(keySize);
        return gen.generateKeyPair();
    }

    public String toPrivateKeyPem(PrivateKey key) {
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(key.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----\n";
    }

    public String toPublicKeyPem(PublicKey key) {
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(key.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----\n";
    }
}
