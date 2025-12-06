package ru.bsh.guarantee.sender.configuration.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.dto.KafkaSenderProperties;

import java.util.HashMap;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = "guarantee.kafka.enabled", havingValue = "true")
public class KafkaConfiguration { // todo: configurationProperties

    @Getter
    private final KafkaSenderProperties properties;

    @Bean
    public ProducerFactory<String, GuaranteeSenderDto> producerFactory() {
        var cfg = new HashMap<String, Object>();

        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        cfg.put(ProducerConfig.CLIENT_ID_CONFIG, properties.getClientId());
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        cfg.put(ProducerConfig.ACKS_CONFIG, properties.getAcks());
        cfg.put(ProducerConfig.RETRIES_CONFIG, properties.getMaxRetries());
        cfg.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, properties.getRqTimeoutMs());
        cfg.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, properties.getDeliveryTimeoutMs());

        var pf = new DefaultKafkaProducerFactory<String, GuaranteeSenderDto>(cfg);
        pf.setValueSerializer(new JsonSerializer<>(new ObjectMapper()));
        return pf;
    }

    @Bean
    public KafkaTemplate<String, GuaranteeSenderDto> kafkaTemplate(
            ProducerFactory<String, GuaranteeSenderDto> factory
    ) {
        return new KafkaTemplate<>(factory);
    }
}
