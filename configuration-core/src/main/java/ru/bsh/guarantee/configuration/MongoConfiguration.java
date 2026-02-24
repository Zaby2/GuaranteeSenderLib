package ru.bsh.guarantee.configuration;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.bsh.guarantee.balancing.BalancingGroupConfiguration;
import ru.bsh.guarantee.balancing.BalancingProvider;
import ru.bsh.guarantee.dto.BufferType;
import ru.bsh.guarantee.dto.MongoSenderDto;
import ru.bsh.guarantee.sender.impl.nosql.MongoDbSender;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "guarantee.nosql.enabled", havingValue = "true")
@ConfigurationProperties("guarantee.nosql.mongo")
@Data
public class MongoConfiguration {

    private String groupName;
    private Long weight;

    @Bean
    @ConfigurationProperties(prefix = "guarantee.nosql.mongo.sender")
    public MongoSenderDto mongoSenderDto() {
        return new MongoSenderDto();
    }

    @Bean("mongoGroup")
    public BalancingGroupConfiguration mongoBalancingGroupConfiguration(MongoDbSender mongoDbSender) {
        var mongoProvider = new BalancingProvider();
        mongoProvider.setName(groupName);
        mongoProvider.setSender(mongoDbSender);
        mongoProvider.setWeight(weight);

        var mongoConf = new BalancingGroupConfiguration();
        mongoConf.setName(groupName);
        mongoConf.setType(BufferType.NOSQL);
        mongoConf.setProvider(List.of(mongoProvider));
        mongoConf.setWeight(Math.toIntExact(weight));
        return mongoConf;
    }
}
