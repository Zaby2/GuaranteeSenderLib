package ru.bsh.guarantee.dto;

import lombok.Data;

import java.util.Map;

@Data
public class MongoSenderDto {

    private Map<String, String> connections;
}
