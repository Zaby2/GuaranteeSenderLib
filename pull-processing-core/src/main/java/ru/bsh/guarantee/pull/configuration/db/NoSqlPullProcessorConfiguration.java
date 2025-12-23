package ru.bsh.guarantee.pull.configuration.db;

import com.mongodb.client.MongoClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import ru.bsh.guarantee.sender.configuration.nosql.MongoConfiguration;

import java.util.List;
import java.util.Map;

@Configuration
@Data
@ConditionalOnProperty(value = "guarantee.nosql.enabled", havingValue = "true")
public class NoSqlPullProcessorConfiguration {

    @Value("${no-sql.puller.limit}")
    private Integer readLimit;
    @Value("${no-sql.cleaner.limit}")
    private Integer cleanLimit;

    private final Map<String, MongoClient> mongoClients;
    private final MongoConfiguration configuration;
    private final List<String> dbNames;

    public NoSqlPullProcessorConfiguration(Map<String, MongoClient> mongoClients,
                                           MongoConfiguration configuration) {
        this.mongoClients = mongoClients;
        this.configuration = configuration;
        this.dbNames = mongoClients.keySet().stream().toList();
    }
}
