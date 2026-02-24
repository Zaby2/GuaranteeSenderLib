package ru.bsh.guarantee.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.bsh.guarantee.balancing.BalancingGroupConfiguration;
import ru.bsh.guarantee.balancing.BalancingProvider;
import ru.bsh.guarantee.dto.BufferType;
import ru.bsh.guarantee.monitoring.GuaranteeMonitoring;
import ru.bsh.guarantee.sender.configuration.http.HttpSenderConfiguration;
import ru.bsh.guarantee.sender.impl.http.HttpSender;

import java.util.Map;

@Configuration
@ConfigurationProperties("guarantee.http")
@Data
public class HttpConfiguration {

    private Map<String, HttpSenderConfiguration> configurations;
    private String groupName;

    @Bean("httpGroup")
    public BalancingGroupConfiguration httpBalancingGroupConfiguration(
            GuaranteeMonitoring monitoring
    ) {
        var providers = configurations.keySet().stream()
                .map(key -> {
                    var config = configurations.get(key);
                    var httpBalancingProvider = new BalancingProvider();
                    httpBalancingProvider.setName(key);
                    httpBalancingProvider.setSender(new HttpSender(config, monitoring));
                    httpBalancingProvider.setWeight(config.getWeight());
                    return httpBalancingProvider;
                }).toList();
        var httpConf = new BalancingGroupConfiguration();
        httpConf.setName(groupName);
        httpConf.setType(BufferType.HTTP);
        httpConf.setProvider(providers);
        return httpConf;
    }
}
