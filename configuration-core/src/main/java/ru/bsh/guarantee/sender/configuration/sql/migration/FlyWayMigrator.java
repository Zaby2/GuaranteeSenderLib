package ru.bsh.guarantee.sender.configuration.sql.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.bsh.guarantee.exception.InternalGuaranteeException;
import ru.bsh.guarantee.sender.configuration.sql.DynamicRoutingDataSource;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "guarantee.sql.enabled", havingValue = "true")
public class FlyWayMigrator {

    private final DynamicRoutingDataSource dataSource;

    @PostConstruct
    public void migrate() {
        dataSource.getResolvedDataSources().forEach((name, ds)
                -> migrate(ds, (String) name));
    }

    private void migrate(DataSource ds, String name) {
        try(var conn = ds.getConnection()) {
            var dbType = conn.getMetaData().getDatabaseProductName().toLowerCase();
            String location;
            if (dbType.contains("mysql")) {
                location = "classpath:db/migrations/mysql";
            } else if (dbType.contains("postgres")) {
                location = "classpath:db/migrations/postgres";
            } else {
                throw new InternalGuaranteeException("Ошибка миграции: тип данных не поддерживается");
            }

            Flyway.configure()
                    .dataSource(ds)
                    .locations(location)
                    .load()
                    .migrate();
            log.info("Миграция в {} завершена", dbType);
        } catch (Exception e) {
            throw new InternalGuaranteeException(e.getMessage());
        }
    }
}
