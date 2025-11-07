package store.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.KeyStoreProvider;
import utils.KeyLoader;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

@Component
@RequiredArgsConstructor
public class PemKeyStoreProvider implements KeyStoreProvider {

    private final String privateKeyPemPath;
    private final String certPemPath;

    @Override
    public PrivateKey getPrivateKey() throws Exception {
        return KeyLoader.loadPrivateKeyFromPemFile(privateKeyPemPath);
    }

    @Override
    public X509Certificate getCertificate() throws Exception {
        return KeyLoader.loadCertFromPemFile(certPemPath);
    }

    @Override
    public PublicKey getPublicKey() throws Exception {
        return getCertificate().getPublicKey();
    }
}
