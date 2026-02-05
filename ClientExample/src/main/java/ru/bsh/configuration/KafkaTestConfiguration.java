package ru.bsh.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.bsh.guarantee.balancing.BalancingGroupConfiguration;
import ru.bsh.guarantee.balancing.BalancingProvider;
import ru.bsh.guarantee.dto.BufferType;
import ru.bsh.guarantee.dto.KafkaSenderProperties;
import ru.bsh.guarantee.pull.dto.KafkaPullProcessorConfigDto;
import ru.bsh.guarantee.sender.impl.broker.KafkaSender;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "guarantee.kafka.enabled", havingValue = "true")
public class KafkaTestConfiguration {

    @Bean
    @ConfigurationProperties("guarantee.kafka.sender")
    public KafkaSenderProperties kafkaSenderProperties() {
        return new KafkaSenderProperties();
    }

    @Bean
    @ConfigurationProperties("guarantee.kafka.puller")
    public KafkaPullProcessorConfigDto kafkaPullProcessorConfigDto() {
        return new KafkaPullProcessorConfigDto();
    }

    @Bean("kafkaGroup")
    public BalancingGroupConfiguration kafkaBalancingGroupConfiguration(KafkaSender kafkaSender) {
        var kafkaProvider = new BalancingProvider();
        kafkaProvider.setName("Kafka");
        kafkaProvider.setSender(kafkaSender);
        kafkaProvider.setWeight(10L);

        var kafkaConf = new BalancingGroupConfiguration();
        kafkaConf.setName("kafka Sender");
        kafkaConf.setType(BufferType.BROKER);
        kafkaConf.setProvider(List.of(kafkaProvider));
        kafkaConf.setWeight(1);
        return kafkaConf;
    }
}
