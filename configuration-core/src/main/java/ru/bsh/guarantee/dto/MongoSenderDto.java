package ru.bsh.guarantee.dto;

import lombok.Data;

import java.util.Map;

@Data
public class MongoSenderDto {

    private final Map<String, String> connections;
}
