package ru.bsh.guarantee.pull.puller.impl.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.bsh.GuaranteeSenderProxyImpl;
import ru.bsh.guarantee.pull.configuration.db.SqlPullProcessorConfiguration;
import ru.bsh.guarantee.pull.converter.GuaranteeSenderEntityToDtoConverter;
import ru.bsh.guarantee.pull.puller.PullProcessor;
import ru.bsh.guarantee.repository.GuaranteeJpaRepository;
import ru.bsh.guarantee.sender.configuration.sql.DbContext;
import service.SignatureService;

import java.util.concurrent.locks.ReentrantLock;

import static ru.bsh.guarantee.pull.utils.Utils.determineCurrentIndex;
import static ru.bsh.guarantee.pull.utils.Utils.verifySignature;

@Service
@Slf4j
@ConditionalOnProperty(name = "guarantee.sql.enabled", havingValue = "true")
public class SqlPullProcessor implements PullProcessor {

    private final SqlPullProcessorConfiguration configuration;
    private final GuaranteeSenderProxyImpl<?> proxy;
    private final SignatureService signatureService;
    private final GuaranteeJpaRepository repository;

    private final ReentrantLock lock = new ReentrantLock();
    private final GuaranteeSenderEntityToDtoConverter converter = new GuaranteeSenderEntityToDtoConverter();

    private Integer currentDataSourceIndex = 0;

    public SqlPullProcessor(SqlPullProcessorConfiguration configuration,
                            GuaranteeSenderProxyImpl<?> proxy,
                            SignatureService signatureService,
                            GuaranteeJpaRepository repository) {
        this.configuration = configuration;
        this.proxy = proxy;
        this.signatureService = signatureService;
        this.repository = repository;
    }

    @Override
    @Scheduled(cron = "${sql.pull.cron}")
    public void pull() {
        if (!lock.tryLock()) {
            return;
        }
        var destinations = configuration.getDestinations();
        try {
            DbContext.set(destinations.get(currentDataSourceIndex));
            var entities = repository.findDataToSend(
                    PageRequest.of(0, configuration.getReadLimit()));

            for (var entity : entities) {
                var dataToResend = converter.convert(entity);
                if (verifySignature(signatureService, dataToResend)) {
                    log.info("Запись с id = {} прошла проверку ЭЦП", dataToResend.getId());
                    proxy.send(dataToResend);
                    entity.setIsSent(true);
                    repository.save(entity);
                } else {
                    log.error("Запись с id = {} не прошла проверку ЭЦП", dataToResend.getId());
                }
            }
        } catch (Exception e) {
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
