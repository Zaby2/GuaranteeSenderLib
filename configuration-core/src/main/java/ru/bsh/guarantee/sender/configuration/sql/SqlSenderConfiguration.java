package ru.bsh.guarantee.sender.configuration.sql;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import ru.bsh.guarantee.dto.SqlSenderProperties;

import javax.sql.DataSource;
import java.util.HashMap;

@Getter
@Configuration
@EnableJpaRepositories(basePackages = "ru.bsh.guarantee.repository")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "guarantee.sql.enabled", havingValue = "true")
public class SqlSenderConfiguration {

    private final SqlSenderProperties properties;

    @Bean
    public DataSource dataSource() {
        var targets = new HashMap<>();

        properties.getPropertiesMap().forEach((name, cfg) -> {
            var ds = new DriverManagerDataSource();
            ds.setUrl(cfg.getUrl());
            ds.setUsername(cfg.getUserName());
            ds.setPassword(cfg.getPassword());
            ds.setDriverClassName(cfg.getDriverClassName());
            targets.put(name, ds);
        });

        var routing = new DynamicRoutingDataSource();
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(targets.values().iterator().next());

        return routing;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        var emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("ru.bsh.guarantee.entity");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        var jpaProps = new HashMap<String, Object>();
        jpaProps.put("hibernate.hbm2ddl.auto", "update");
        jpaProps.put("hibernate.show_sql", false);
        jpaProps.put("hibernate.format_sql", true);

        emf.setJpaPropertyMap(jpaProps);

        return emf;
    }
}
