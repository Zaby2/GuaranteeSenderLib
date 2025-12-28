package ru.bsh.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.bsh.guarantee.balancing.BalancingGroupConfiguration;
import ru.bsh.guarantee.balancing.BalancingProvider;
import ru.bsh.guarantee.dto.BufferType;
import ru.bsh.guarantee.monitoring.GuaranteeMonitoring;
import ru.bsh.guarantee.sender.configuration.http.HttpSenderConfiguration;
import ru.bsh.guarantee.sender.impl.http.HttpSender;

import java.util.List;

@Configuration
public class HttpTestConfiguration {

    @Bean("http1")
    @ConfigurationProperties(prefix = "guarantee.http1")
    public HttpSenderConfiguration httpSenderConfiguration1() {
        return new HttpSenderConfiguration();
    }

    @Bean("http2")
    @ConfigurationProperties(prefix = "guarantee.http2")
    public HttpSenderConfiguration httpSenderConfiguration2() {
        return new HttpSenderConfiguration();
    }

    @Bean("httpGroup")
    public BalancingGroupConfiguration httpBalancingGroupConfiguration(
            @Qualifier("http1") HttpSenderConfiguration httpSenderConfiguration1,
            @Qualifier("http2") HttpSenderConfiguration httpSenderConfiguration2,
            GuaranteeMonitoring monitoring
    ) {
        var httpBalancingProvider = new BalancingProvider();
        httpBalancingProvider.setName("Http1");
        httpBalancingProvider.setSender(new HttpSender(httpSenderConfiguration1, monitoring));
        httpBalancingProvider.setWeight(10L);

        var httpBalancingProvider2 = new BalancingProvider();
        httpBalancingProvider2.setName("Http2");
        httpBalancingProvider2.setSender(new HttpSender(httpSenderConfiguration2, monitoring));
        httpBalancingProvider2.setWeight(15L);

        var httpConf = new BalancingGroupConfiguration();
        httpConf.setName("Http");
        httpConf.setType(BufferType.HTTP);
        httpConf.setProvider(List.of(httpBalancingProvider, httpBalancingProvider2));
        return httpConf;
    }
}
