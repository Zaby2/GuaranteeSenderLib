package ru.bsh.guarantee.configuration;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import service.SignatureService;
import store.impl.JksKeyStoreProvider;

@Configuration
@ConfigurationProperties("guarantee.signature")
@ConditionalOnMissingBean(SignaturePemConfiguration.class)
@Data
public class SignatureJksConfiguration {

    private String jksPath;
    private String storePassword;
    private String alias;

    @Bean
    public SignatureService signatureService() {
        return new SignatureService(
                new JksKeyStoreProvider(jksPath, storePassword.toCharArray(), alias));
    }
}
