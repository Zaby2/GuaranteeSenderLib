package ru.bsh.guarantee.pull.cleaner.sql;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.bsh.guarantee.pull.cleaner.CleanProcessor;
import ru.bsh.guarantee.pull.configuration.db.SqlPullProcessorConfiguration;
import ru.bsh.guarantee.repository.GuaranteeJpaRepository;
import ru.bsh.guarantee.sender.configuration.sql.DbContext;

import java.util.concurrent.locks.ReentrantLock;

import static ru.bsh.guarantee.pull.utils.Utils.determineCurrentIndex;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "guarantee.sql.enabled", havingValue = "true")
public class SqlCleanProcessor implements CleanProcessor {

    private final SqlPullProcessorConfiguration configuration;
    private final GuaranteeJpaRepository repository;

    private final ReentrantLock lock = new ReentrantLock();

    private Integer currentDataSourceIndex = 0;

    @Override
    @Scheduled(cron = "${sql.clean.cron}")
    public void clean() {
        if (!lock.tryLock()) {
            return;
        }

        var destinations = configuration.getDestinations();
        var currentDestination = destinations.get(currentDataSourceIndex);
        DbContext.set(currentDestination);

        try {
            var result = repository.deleteBatchByStatusTrue(configuration.getCleanLimit());
            log.info("Удалено {} записей из SQL {}", result, currentDestination);
        } catch (Exception e) {
            log.error("Ошибка {} удаления записей из SQL {}", e.getMessage(),
                    currentDestination);
        } finally {
            currentDataSourceIndex = determineCurrentIndex(currentDataSourceIndex,
                    destinations.size());
            DbContext.clear();
            lock.unlock();
        }
    }
}
