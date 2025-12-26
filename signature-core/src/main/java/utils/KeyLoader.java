package utils;

import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class KeyLoader {

    private static PrivateKey loadPrivateKeyFromPem(String pem) throws GeneralSecurityException {
        String normalized = pem
                .replaceAll("-----BEGIN (.*)PRIVATE KEY-----", "")
                .replaceAll("-----END (.*)PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        try {
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (InvalidKeySpecException e) {
            return KeyFactory.getInstance("EC").generatePrivate(spec);
        }
    }

    public static X509Certificate loadCertFromPem(String pem) throws CertificateException {
        String normalized = pem
                .replaceAll("-----BEGIN CERTIFICATE-----", "")
                .replaceAll("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        byte[] certBytes = Base64.getDecoder().decode(normalized);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }


    public static PrivateKey loadPrivateKeyFromPemFile(String path) throws GeneralSecurityException, IOException {
        var classPath = new ClassPathResource(path);
        String pemContent = new String(classPath.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        return loadPrivateKeyFromPem(pemContent);
    }

    public static X509Certificate loadCertFromPemFile(String path) throws CertificateException, IOException {
        var classPath = new ClassPathResource(path);
        String pemContent = new String(classPath.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        return loadCertFromPem(pemContent);
    }

    public static String certToPem(X509Certificate cert) throws CertificateEncodingException {
        String encoded = Base64.getEncoder().encodeToString(cert.getEncoded());
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN CERTIFICATE-----\n");
        for (int i = 0; i < encoded.length(); i += 64) {
            sb.append(encoded, i, Math.min(i + 64, encoded.length())).append("\n");
        }
        sb.append("-----END CERTIFICATE-----\n");
        return sb.toString();
    }

    public static JksResult loadFromJks(InputStream jksStream,
                                        char[] storePassword,
                                        String alias,
                                        char[] keyPassword)
            throws IOException, GeneralSecurityException {

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(jksStream, storePassword);

        Key key = keyStore.getKey(alias, keyPassword);
        if (!(key instanceof PrivateKey)) {
            throw new KeyStoreException("Alias does not contain a private key: " + alias);
        }
        var cert = keyStore.getCertificate(alias);
        if (cert == null) {
            throw new KeyStoreException("No certificate found for alias: " + alias);
        }

        return new JksResult((PrivateKey) key, (X509Certificate) cert);
    }

    public static class JksResult {
        private final PrivateKey privateKey;
        private final X509Certificate certificate;

        public JksResult(PrivateKey privateKey, X509Certificate certificate) {
            this.privateKey = privateKey;
            this.certificate = certificate;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }

        public X509Certificate getCertificate() {
            return certificate;
        }
    }
}