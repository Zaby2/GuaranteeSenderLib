package ru.bsh.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
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

    @Bean("http1")
    @ConfigurationProperties(prefix = "http1")
    public HttpSenderConfiguration httpSenderConfiguration1() {
        return new HttpSenderConfiguration();
    }

    @Bean("http2")
    @ConfigurationProperties(prefix = "http2")
    public HttpSenderConfiguration httpSenderConfiguration2() {
        return new HttpSenderConfiguration();
    }

    @Bean
    public List<BalancingGroupConfiguration> balancingGroupConfiguration(
            @Qualifier("http1") HttpSenderConfiguration httpSenderConfiguration1,
            @Qualifier("http2") HttpSenderConfiguration httpSenderConfiguration2
    ) {
        var httpBalancingProvider = new BalancingProvider();
        httpBalancingProvider.setName("Http1");
        httpBalancingProvider.setSender(new HttpSender(httpSenderConfiguration1));
        httpBalancingProvider.setWeight(10L);

        var httpBalancingProvider2 = new BalancingProvider();
        httpBalancingProvider2.setName("Http2");
        httpBalancingProvider2.setSender(new HttpSender(httpSenderConfiguration2));
        httpBalancingProvider2.setWeight(15L);

        var httpConf = new BalancingGroupConfiguration();
        httpConf.setName("Http");
        httpConf.setType(BufferType.HTTP);
        httpConf.setProvider(List.of(httpBalancingProvider, httpBalancingProvider2));
        return List.of(httpConf);
    }


    @Bean
    public GuaranteeSenderConfiguration guaranteeSenderConfiguration(
            List<BalancingGroupConfiguration> balancingGroupConfigurations
    ) {
        var signatureService = new SignatureService(
                new PemKeyStoreProvider("path1", "path2"));
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
