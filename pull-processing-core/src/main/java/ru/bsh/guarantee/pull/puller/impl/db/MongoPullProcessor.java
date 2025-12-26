package ru.bsh.guarantee.pull.puller.impl.db;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.bsh.GuaranteeSenderProxyImpl;
import ru.bsh.guarantee.pull.configuration.db.NoSqlPullProcessorConfiguration;
import ru.bsh.guarantee.pull.converter.GuaranteeSenderMongoToDtoConverter;
import ru.bsh.guarantee.pull.puller.PullProcessor;
import service.SignatureService;

import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import static ru.bsh.guarantee.pull.utils.Utils.determineCurrentIndex;
import static ru.bsh.guarantee.pull.utils.Utils.verifySignature;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "guarantee.nosql.enabled", havingValue = "true")
public class MongoPullProcessor implements PullProcessor {

    private final NoSqlPullProcessorConfiguration configuration;
    private final GuaranteeSenderProxyImpl<?> proxy;
    private final SignatureService signatureService;

    private final GuaranteeSenderMongoToDtoConverter converter = new GuaranteeSenderMongoToDtoConverter();

    private Integer currentDbIndex = 0;

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    @Scheduled(cron = "${no-sql.pull.cron}")
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
                if (verifySignature(signatureService, dataToSend,
                        new String(document.getString("signature")))) {

                    log.info("Запись с id = {} прошла проверку ЭЦП", objectId);
                    proxy.send(dataToSend);

                    collection.updateOne(
                            Filters.eq("_id", objectId),
                            Updates.combine(Updates.set("isSent", true),
                                    Updates.set("polledAt", new Date()))
                    );
                } else {
                    log.error("Запись с id = {} не прошла проверку ЭЦП", objectId);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка {} при доотправке данных из MongoDb {}",
                    e.getMessage(), currentDbName);
        } finally {
            currentDbIndex = determineCurrentIndex(currentDbIndex,
                    configuration.getDbNames().size());
            lock.unlock();
        }
    }
}