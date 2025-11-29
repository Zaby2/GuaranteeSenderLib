package ru.bsh.guarantee.sender.configuration.nosql;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = "guarantee.nosql.enabled", havingValue = "true")
public class MongoConfiguration {

    private final Map<String, String> connections;

    @Bean
    public Map<String, MongoClient> mongoClients() {
        var result = new HashMap<String, MongoClient>();

        connections.forEach((name, uri) -> {
            result.put(name, MongoClients.create(uri));
        });
        return result;
    }
}
