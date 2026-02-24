package ru.bsh.guarantee.dto;

import lombok.Data;

@Data
public class KafkaPullProcessorConfigDto {

    private String bootstrapServers;
    private String groupId;
    private String autoOffsetReset;
    private int maxPollRecords;
}
