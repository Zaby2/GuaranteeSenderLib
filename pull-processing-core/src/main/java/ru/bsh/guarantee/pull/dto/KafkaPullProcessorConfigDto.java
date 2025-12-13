package ru.bsh.guarantee.pull.dto;

import lombok.Data;

@Data
public class KafkaPullProcessorConfigDto {

    private String bootstrapServers;
    private String groupId;
    private String keyDeserializer;
    private String valueDeserializer;
    private String autoOffsetReset;
    private boolean enableAutoCommit;
    private int maxPollRecords;
}
