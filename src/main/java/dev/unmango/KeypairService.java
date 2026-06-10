package dev.unmango;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

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
        return toPem("PRIVATE KEY", key);
    }

    public String toPublicKeyPem(PublicKey key) {
        return toPem("PUBLIC KEY", key);
    }

    private String toPem(String type, Key key) {
        StringWriter sw = new StringWriter();
        try (PemWriter writer = new PemWriter(sw)) {
            writer.writeObject(new PemObject(type, key.getEncoded()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sw.toString();
    }
}
