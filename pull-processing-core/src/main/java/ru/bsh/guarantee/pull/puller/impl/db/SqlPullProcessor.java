package ru.bsh.guarantee.pull.puller.impl.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bsh.GuaranteeSenderProxyImpl;
import ru.bsh.guarantee.entity.SqlGuaranteeEntity;
import ru.bsh.guarantee.monitoring.GuaranteeMonitoring;
import ru.bsh.guarantee.pull.configuration.db.SqlPullProcessorConfiguration;
import ru.bsh.guarantee.pull.converter.GuaranteeSenderEntityToDtoConverter;
import ru.bsh.guarantee.pull.puller.PullProcessor;
import ru.bsh.guarantee.pull.transaction.TransactionalOperator;
import ru.bsh.guarantee.repository.GuaranteeJpaRepository;
import ru.bsh.guarantee.sender.configuration.sql.DbContext;
import service.SignatureService;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static ru.bsh.guarantee.monitoring.MonitoringConstants.SIGNATURE_CHECK;
import static ru.bsh.guarantee.monitoring.MonitoringConstants.SQL_PULLER;
import static ru.bsh.guarantee.pull.utils.Utils.determineCurrentIndex;
import static ru.bsh.guarantee.pull.utils.Utils.verifySignature;

@Service
@Slf4j
@ConditionalOnProperty(name = "guarantee.sql.enabled", havingValue = "true")
public class SqlPullProcessor implements PullProcessor {

    private final SqlPullProcessorConfiguration configuration;
    private final Map<String, GuaranteeSenderProxyImpl<?>> proxyMap;
    private final SignatureService signatureService;
    private final GuaranteeJpaRepository repository;
    private final GuaranteeMonitoring monitoring;
    private final TransactionalOperator transactionalOperator;

    private final ReentrantLock lock = new ReentrantLock();
    private final GuaranteeSenderEntityToDtoConverter converter = new GuaranteeSenderEntityToDtoConverter();

    private Integer currentDataSourceIndex = 0;

    public SqlPullProcessor(SqlPullProcessorConfiguration configuration,
                            List<GuaranteeSenderProxyImpl<?>> proxy,
                            SignatureService signatureService,
                            GuaranteeJpaRepository repository,
                            GuaranteeMonitoring monitoring,
                            TransactionalOperator transactionalOperator) {
        this.configuration = configuration;
        this.signatureService = signatureService;
        this.repository = repository;
        this.monitoring = monitoring;
        this.transactionalOperator = transactionalOperator;

        var map = new HashMap<String, GuaranteeSenderProxyImpl<?>>();
        proxy.forEach(p -> map.put(p.getPlayLoadType().getName(), p));
        this.proxyMap = map;
    }

    @Override
    @Scheduled(cron = "${guarantee.sql.puller.cron}")
    public void pull() {
        if (!lock.tryLock()) {
            return;
        }
        var destinations = configuration.getDestinations();
        try {
            DbContext.set(destinations.get(currentDataSourceIndex));
            var entities = transactionalOperator.getEntities();

            for (var entity : entities) {
                var dataToSend = converter.convert(entity);
                var proxy = proxyMap.getOrDefault(dataToSend.getRequestType(), null);
                if (proxy == null) {
                    log.error("Для записи с id = {} не найден подходящий прокси", entity.getId());
                    continue;
                }
                if (verifySignature(signatureService, dataToSend,
                        new String(entity.getSignature(), StandardCharsets.UTF_8))) {

                    log.info("Запись с id = {} прошла проверку ЭЦП", entity.getId());
                    proxy.send(dataToSend);
                } else {
                    monitoring.fail(SIGNATURE_CHECK.getLayer(), SIGNATURE_CHECK.getOperation());
                    log.error("Запись с id = {} не прошла проверку ЭЦП", entity.getId());
                }
                entity.setPolledAt(new Date());
                entity.setIsSent(true);
                repository.save(entity);
            }
            monitoring.success(SQL_PULLER.getLayer(), SQL_PULLER.getOperation());
        } catch (Exception e) {
            monitoring.fail(SQL_PULLER.getLayer(), SQL_PULLER.getOperation());
            log.error("Ошибка {} при доотправке данных из SQL {}",
                    e.getMessage(), destinations.get(currentDataSourceIndex));
        } finally {
            currentDataSourceIndex = determineCurrentIndex(currentDataSourceIndex,
                    configuration.getDestinations().size());
            DbContext.clear();
            lock.unlock();
        }
    }
}
