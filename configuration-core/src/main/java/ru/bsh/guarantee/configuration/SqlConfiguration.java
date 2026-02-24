package ru.bsh.guarantee.configuration;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.bsh.guarantee.balancing.BalancingGroupConfiguration;
import ru.bsh.guarantee.balancing.BalancingProvider;
import ru.bsh.guarantee.dto.BufferType;
import ru.bsh.guarantee.dto.SqlSenderProperties;
import ru.bsh.guarantee.sender.impl.sql.SqlSender;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "guarantee.sql.enabled", havingValue = "true")
@ConfigurationProperties("guarantee.sql")
@Data
public class SqlConfiguration {

    private String groupName;
    private Long weight;

    @Bean
    @ConfigurationProperties(prefix = "guarantee.sql.sender")
    public SqlSenderProperties sqlSenderProperties() {
        return new SqlSenderProperties();
    }

    @Bean("sqlGroup")
    public BalancingGroupConfiguration sqlBalancingGroupConfiguration(SqlSender sqlSender) {
        var sqlProvider = new BalancingProvider();
        sqlProvider.setName(groupName);
        sqlProvider.setSender(sqlSender);
        sqlProvider.setWeight(weight);

        var sqlConf = new BalancingGroupConfiguration();
        sqlConf.setName(groupName);
        sqlConf.setType(BufferType.SQL);
        sqlConf.setProvider(List.of(sqlProvider));
        sqlConf.setWeight(Math.toIntExact(weight));
        return sqlConf;
    }
}
