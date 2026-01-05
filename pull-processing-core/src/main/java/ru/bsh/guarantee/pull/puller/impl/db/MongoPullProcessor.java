package ru.bsh.guarantee.pull.puller.impl.db;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.bsh.GuaranteeSenderProxyImpl;
import ru.bsh.guarantee.monitoring.GuaranteeMonitoring;
import ru.bsh.guarantee.pull.configuration.db.NoSqlPullProcessorConfiguration;
import ru.bsh.guarantee.pull.converter.GuaranteeSenderMongoToDtoConverter;
import ru.bsh.guarantee.pull.puller.PullProcessor;
import service.SignatureService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static ru.bsh.guarantee.monitoring.MonitoringConstants.NO_SQL_PULLER;
import static ru.bsh.guarantee.monitoring.MonitoringConstants.SIGNATURE_CHECK;
import static ru.bsh.guarantee.pull.utils.Utils.determineCurrentIndex;
import static ru.bsh.guarantee.pull.utils.Utils.verifySignature;

@Service
@Slf4j
@ConditionalOnProperty(value = "guarantee.nosql.enabled", havingValue = "true")
public class MongoPullProcessor implements PullProcessor {

    private final NoSqlPullProcessorConfiguration configuration;
    private final Map<String, GuaranteeSenderProxyImpl<?>> proxyMap;
    private final SignatureService signatureService;
    private final GuaranteeMonitoring monitoring;

    private final GuaranteeSenderMongoToDtoConverter converter = new GuaranteeSenderMongoToDtoConverter();

    private Integer currentDbIndex = 0;

    private final ReentrantLock lock = new ReentrantLock();

    public MongoPullProcessor(NoSqlPullProcessorConfiguration configuration,
                              List<GuaranteeSenderProxyImpl<?>> proxy,
                              SignatureService signatureService,
                              GuaranteeMonitoring monitoring) {
        this.configuration = configuration;
        this.signatureService = signatureService;
        this.monitoring = monitoring;

        var map = new HashMap<String, GuaranteeSenderProxyImpl<?>>();
        proxy.forEach(p -> map.put(p.getPlayLoadType().getName(), p));
        this.proxyMap = map;
    }

    @Override
    @Scheduled(cron = "${guarantee.nosql.mongo.puller.cron}")
    public void pull() {
        if (!lock.tryLock()) {
            return;
        }

        var currentDbName = configuration.getDbNames().get(currentDbIndex);
        var currentClient = configuration.getMongoClients().get(currentDbName);

        try {
            var collection = currentClient.getDatabase(currentDbName)
                    .getCollection("guarantee_data");

            var iterableData = collection
                    .find(Filters.eq("isSent", false))
                    .sort(Sorts.descending("createdAt"))
                    .limit(configuration.getReadLimit());
            for (var document : iterableData) {
                var dataToSend = converter.convert(document);
                var objectId = document.getObjectId("_id");
                var proxy = proxyMap.getOrDefault(dataToSend.getRequestType(), null);
                if (proxy == null) {
                    log.error("Для записи с id = {} не найден подходящий прокси", objectId);
                    continue;
                }
                if (verifySignature(signatureService, dataToSend,
                        new String(document.getString("signature")))) {

                    log.info("Запись с id = {} прошла проверку ЭЦП", objectId);
                    proxy.send(dataToSend);

                    collection.updateOne(
                            Filters.eq("_id", objectId),
                            Updates.combine(Updates.set("isSent", true),
                                    Updates.set("polledAt", new Date()))
                    );
                    monitoring.success(NO_SQL_PULLER.getLayer(), NO_SQL_PULLER.getOperation());
                } else {
                    monitoring.fail(SIGNATURE_CHECK.getLayer(), SIGNATURE_CHECK.getOperation());
                    log.error("Запись с id = {} не прошла проверку ЭЦП", objectId);
                }
            }
        } catch (Exception e) {
            monitoring.fail(NO_SQL_PULLER.getLayer(), NO_SQL_PULLER.getOperation());
            log.error("Ошибка {} при доотправке данных из MongoDb {}",
                    e.getMessage(), currentDbName);
        } finally {
            currentDbIndex = determineCurrentIndex(currentDbIndex,
                    configuration.getDbNames().size());
            lock.unlock();
        }
    }
}