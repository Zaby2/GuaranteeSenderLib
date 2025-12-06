package ru.bsh.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.bsh.GuaranteeSenderProxyImpl;
import ru.bsh.dto.Request;
import ru.bsh.guarantee.balancing.BalancingGroupConfiguration;
import ru.bsh.guarantee.balancing.BalancingProvider;
import ru.bsh.guarantee.balancing.CircuitBreakerConfiguration;
import ru.bsh.guarantee.configuration.GuaranteeSenderConfiguration;
import ru.bsh.guarantee.dto.BufferType;
import ru.bsh.guarantee.sender.configuration.http.HttpSenderConfiguration;
import ru.bsh.guarantee.sender.impl.http.HttpSender;
import service.SignatureService;
import store.impl.PemKeyStoreProvider;

import java.util.List;

@Configuration
public class TestConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "http")
    public HttpSenderConfiguration httpSenderConfiguration() {
        return new HttpSenderConfiguration();
    }

    @Bean
    public List<BalancingGroupConfiguration> balancingGroupConfiguration(
            HttpSenderConfiguration httpSenderConfiguration
    ) {
        var httpBalancingProvider = new BalancingProvider();
        httpBalancingProvider.setName("Http1");
        httpBalancingProvider.setSender(new HttpSender(httpSenderConfiguration));
        httpBalancingProvider.setWeight(10L);

        var httpConf = new BalancingGroupConfiguration();
        httpConf.setName("Http");
        httpConf.setType(BufferType.HTTP);
        httpConf.setProvider(List.of(httpBalancingProvider));
        return List.of(httpConf);
    }


    @Bean
    public GuaranteeSenderConfiguration guaranteeSenderConfiguration(
            List<BalancingGroupConfiguration> balancingGroupConfigurations
    ) {
        var signatureService = new SignatureService(
                new PemKeyStoreProvider("path1", "path2"));
        var cb = new CircuitBreakerConfiguration(100L, 100L);
        return new GuaranteeSenderConfiguration(signatureService, cb, balancingGroupConfigurations);
    }

    @Bean
    public GuaranteeSenderProxyImpl<Request> guaranteeSenderProxy(GuaranteeSenderConfiguration configuration) {
        return new GuaranteeSenderProxyImpl<>(configuration);
    }
}
