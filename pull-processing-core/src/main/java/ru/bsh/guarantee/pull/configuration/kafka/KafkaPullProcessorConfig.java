package ru.bsh.guarantee.pull.configuration.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.pull.dto.KafkaPullProcessorConfigDto;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = "guarantee.kafka.enabled", havingValue = "true")
public class KafkaPullProcessorConfig {

    private final KafkaPullProcessorConfigDto configDto;

    @Bean
    public KafkaConsumer<String, GuaranteeSenderDto> kafkaConsumer() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, configDto.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, configDto.getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, configDto.getKeyDeserializer());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, configDto.getValueDeserializer());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, configDto.getAutoOffsetReset());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, configDto.isEnableAutoCommit());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, configDto.getMaxPollRecords());
        return new KafkaConsumer<>(props);
    }
}
