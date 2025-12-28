package ru.bsh.guarantee.pull.cleaner.nosql;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.bsh.guarantee.monitoring.GuaranteeMonitoring;
import ru.bsh.guarantee.pull.cleaner.CleanProcessor;
import ru.bsh.guarantee.pull.configuration.db.NoSqlPullProcessorConfiguration;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import static ru.bsh.guarantee.monitoring.MonitoringConstants.NO_SQL_CLEANER;
import static ru.bsh.guarantee.pull.utils.Utils.determineCurrentIndex;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "guarantee.nosql.enabled", havingValue = "true")
public class MongoDbCleanProcessor implements CleanProcessor {

    private final NoSqlPullProcessorConfiguration configuration;
    private final GuaranteeMonitoring monitoring;

    private Integer currentDbIndex = 0;

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    @Scheduled(cron = "${guarantee.nosql.mongo.cleaner.cron}")
    public void clean() {
        if (!lock.tryLock()) {
            return;
        }

        var currentDbName = configuration.getDbNames().get(currentDbIndex);
        var currentClient = configuration.getMongoClients().get(currentDbName);

        try {
            var collection = currentClient.getDatabase(currentDbName)
                    .getCollection("guarantee_data");

            var idsToDelete = new ArrayList<>();
            for (var document : collection.find(Filters.eq("isSent", true))
                    .sort(Sorts.descending("createdAt"))
                    .limit(configuration.getCleanLimit())) {
                idsToDelete.add(document.getObjectId("_id"));
            }

            if (!idsToDelete.isEmpty()) {
                collection.deleteMany(Filters.in("_id", idsToDelete));
            }
            log.info("Удалено {} записей из MongoDb {} - {}", idsToDelete.size(), currentDbName,
                    idsToDelete);
            monitoring.success(NO_SQL_CLEANER.getLayer(), NO_SQL_CLEANER.getOperation());
        } catch (Exception e) {
            monitoring.fail(NO_SQL_CLEANER.getLayer(), NO_SQL_CLEANER.getOperation());
            log.error("Ошибка {} удаления записей через MongoDb {}", e.getMessage(), currentDbName);
        } finally {
            currentDbIndex = determineCurrentIndex(currentDbIndex,
                    configuration.getDbNames().size());
            lock.unlock();
        }
    }
}
