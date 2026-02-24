package ru.bsh.guarantee.configuration;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import service.SignatureService;
import store.impl.PemKeyStoreProvider;

@Configuration
@ConfigurationProperties("guarantee.signature")
@ConditionalOnProperty(name = "guarantee.signature.pem", havingValue = "true")
@Data
public class SignaturePemConfiguration {

    private String keyPath;
    private String certPath;

    @Bean
    public SignatureService signatureService() {
        return new SignatureService(
                new PemKeyStoreProvider(keyPath, certPath));
    }
}
