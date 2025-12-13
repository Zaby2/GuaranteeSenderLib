package service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import store.KeyStoreProvider;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SignatureService {

    private final KeyStoreProvider keyStoreProvider;

    public String sign(String data) {
        var bytes = data.getBytes(StandardCharsets.UTF_8);
        try {
            var privateKey = keyStoreProvider.getPrivateKey();
            var sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(bytes);
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
            return "";
        }
    }

    public Boolean verify(String data, String signatureBase64) {
        var bytes = data.getBytes(StandardCharsets.UTF_8);
        var sigBytes = Base64.getDecoder().decode(signatureBase64);
        try {
            var publicKey = keyStoreProvider.getPublicKey();
            var sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(bytes);
            return sig.verify(sigBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
