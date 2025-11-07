package store;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public interface KeyStoreProvider {

    PrivateKey getPrivateKey() throws Exception;
    X509Certificate getCertificate() throws Exception;
    PublicKey getPublicKey() throws Exception;
}
