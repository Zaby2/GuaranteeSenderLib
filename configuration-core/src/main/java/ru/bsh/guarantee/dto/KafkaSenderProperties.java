package ru.bsh.guarantee.dto;

import lombok.Data;

@Data
public class KafkaSenderProperties {

    private String topic;
    private String bootstrapServers;
    private String clientId;
    private String acks;
    private Integer maxRetries;
    private Integer rqTimeoutMs;
    private Integer deliveryTimeoutMs;
}
