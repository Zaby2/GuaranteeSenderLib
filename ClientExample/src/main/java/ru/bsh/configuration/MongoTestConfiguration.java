package ru.bsh.configuration;

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
public class MongoTestConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "guarantee.nosql.mongo.sender")
    public MongoSenderDto mongoSenderDto() {
        return new MongoSenderDto();
    }

    @Bean("mongoGroup")
    public BalancingGroupConfiguration mongoBalancingGroupConfiguration(MongoDbSender mongoDbSender) {
        var mongoProvider = new BalancingProvider();
        mongoProvider.setName("MONGO-1");
        mongoProvider.setSender(mongoDbSender);
        mongoProvider.setWeight(15L);

        var mongoConf = new BalancingGroupConfiguration();
        mongoConf.setName("Mongo Sender");
        mongoConf.setType(BufferType.NOSQL);
        mongoConf.setProvider(List.of(mongoProvider));
        mongoConf.setWeight(4);
        return mongoConf;
    }
}
