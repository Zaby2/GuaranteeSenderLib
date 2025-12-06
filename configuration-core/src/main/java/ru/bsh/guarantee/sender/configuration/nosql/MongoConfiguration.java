package ru.bsh.guarantee.sender.configuration.nosql;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.bsh.guarantee.dto.MongoSenderDto;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = "guarantee.nosql.enabled", havingValue = "true")
public class MongoConfiguration {

    @Getter
    private final MongoSenderDto mongoSenderDto;

    @Bean
    public Map<String, MongoClient> mongoClients() {
        var result = new HashMap<String, MongoClient>();

        mongoSenderDto.getConnections().forEach((name, uri) -> {
            result.put(name, MongoClients.create(uri));
        });
        return result;
    }
}
