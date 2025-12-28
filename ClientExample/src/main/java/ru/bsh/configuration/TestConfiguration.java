package ru.bsh.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.bsh.GuaranteeSenderProxyImpl;
import ru.bsh.dto.Request;
import ru.bsh.guarantee.balancing.BalancingGroupConfiguration;
import ru.bsh.guarantee.balancing.CircuitBreakerConfiguration;
import ru.bsh.guarantee.configuration.GuaranteeSenderConfiguration;
import service.SignatureService;
import store.impl.PemKeyStoreProvider;

import java.util.List;

@Configuration
public class TestConfiguration {


    @Bean
    public List<BalancingGroupConfiguration> balancingGroupConfigurations(
            @Qualifier("httpGroup") BalancingGroupConfiguration httpBg,
            @Qualifier("mongoGroup") BalancingGroupConfiguration mongoBg,
            @Qualifier("sqlGroup") BalancingGroupConfiguration sqlBg
    ) {
        return List.of(httpBg, sqlBg, mongoBg);
    }

    @Bean
    public SignatureService signatureService() {
        return new SignatureService(
                new PemKeyStoreProvider("key.pem",
                        "cert.pem"));
    }

    @Bean
    public GuaranteeSenderConfiguration guaranteeSenderConfiguration(
            List<BalancingGroupConfiguration> balancingGroupConfigurations,
            SignatureService signatureService
    ) {
        var cb = new CircuitBreakerConfiguration(1L, 5L);
        return new GuaranteeSenderConfiguration(signatureService, cb, balancingGroupConfigurations);
    }

    @Bean
    public GuaranteeSenderProxyImpl<Request> guaranteeSenderProxy(
            GuaranteeSenderConfiguration configuration
    ) {
        return new GuaranteeSenderProxyImpl<>(configuration);
    }
}
