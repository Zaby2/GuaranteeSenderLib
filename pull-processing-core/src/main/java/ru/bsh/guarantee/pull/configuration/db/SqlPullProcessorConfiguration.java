package ru.bsh.guarantee.pull.configuration.db;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.bsh.guarantee.sender.configuration.sql.SqlSenderConfiguration;

import java.util.List;

@Component
@Data
@ConditionalOnProperty(name = "guarantee.sql.enabled", havingValue = "true")
public class SqlPullProcessorConfiguration {

    @Value("${guarantee.sql.puller.limit}")
    private Integer readLimit;
    @Value("${guarantee.sql.cleaner.limit}")
    private Integer cleanLimit;

    private final List<String> destinations;

    public SqlPullProcessorConfiguration(SqlSenderConfiguration sqlConfiguration) {
        this.destinations = sqlConfiguration.getProperties().getPropertiesMap().keySet().stream().toList();
    }
}
