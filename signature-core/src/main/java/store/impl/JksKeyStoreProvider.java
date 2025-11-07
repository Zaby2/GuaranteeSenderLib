package store.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.KeyStoreProvider;
import utils.KeyLoader;

import java.io.FileInputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

@Component
@RequiredArgsConstructor
public class JksKeyStoreProvider implements KeyStoreProvider {

    private final String jksPath;
    private final char[] storePassword;
    private final String alias;

    @Override
    public PrivateKey getPrivateKey() throws Exception {
        try (FileInputStream fis = new FileInputStream(jksPath)) {
            return KeyLoader.loadFromJks(fis, storePassword, alias, storePassword).getPrivateKey();
        }
    }

    @Override
    public X509Certificate getCertificate() throws Exception {
        try (FileInputStream fis = new FileInputStream(jksPath)) {
            return KeyLoader.loadFromJks(fis, storePassword, alias, storePassword).getCertificate();
        }
    }

    @Override
    public PublicKey getPublicKey() throws Exception {
        return getCertificate().getPublicKey();
    }
}
