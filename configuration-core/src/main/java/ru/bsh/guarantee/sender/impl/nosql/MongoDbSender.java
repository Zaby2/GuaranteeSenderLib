package ru.bsh.guarantee.sender.impl.nosql;

import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.bsh.guarantee.converter.GuaranteeSenderDtoToMongoConverter;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.exception.InternalGuaranteeException;
import ru.bsh.guarantee.monitoring.GuaranteeMonitoring;
import ru.bsh.guarantee.sender.GuaranteeSender;

import java.util.Map;
import java.util.Objects;

import static ru.bsh.guarantee.monitoring.MonitoringConstants.NO_SQL_SENDER;
import static ru.bsh.guarantee.monitoring.MonitoringConstants.TRANSPORT;

@Service
@Slf4j
@ConditionalOnProperty(value = "guarantee.nosql.enabled", havingValue = "true")
public class MongoDbSender implements GuaranteeSender {

    private final Map<String, MongoClient> mongoClients;
    private final GuaranteeMonitoring monitoring;

    private final GuaranteeSenderDtoToMongoConverter converter = new GuaranteeSenderDtoToMongoConverter();

    public MongoDbSender(Map<String, MongoClient> mongoClients,
                         GuaranteeMonitoring monitoring) {
        this.mongoClients = mongoClients;
        this.monitoring = monitoring;
        log.info("Инициализирован MongoSender точки отправки: {}", mongoClients.keySet());
    }

    @Override
    public void send(GuaranteeSenderDto dataToSend) {
        var isSend = false;
        for (var name : mongoClients.keySet()) {
            MongoClient client;
            try {
                client = mongoClients.get(name);
            } catch (Exception e) {
                monitoring.fail(NO_SQL_SENDER.getLayer(), NO_SQL_SENDER.getOperation());
                log.error("При попытке переключения между Монго " +
                        "транспортами возникла ошибка {}, веротяна ошибка конфигурации", e.getMessage());
                continue;
            }

            try {
                client.getDatabase(name)
                        .getCollection("guarantee_data")
                        .insertOne(Objects.requireNonNull(converter.convert(dataToSend)));

                monitoring.success(NO_SQL_SENDER.getLayer(), NO_SQL_SENDER.getOperation());
                log.info("Отправка через MongoDb {} транспорт произошла успешно", name);
                isSend = true;
                break;
            } catch (Exception e) {
                monitoring.fail(NO_SQL_SENDER.getLayer(), NO_SQL_SENDER.getOperation());
                log.error("Отправка через MongoDb {} транспорт произошла с ошибкой {}", name, e.getMessage());
            }
        }
        if (!isSend) {
            monitoring.fail(TRANSPORT.getLayer(), TRANSPORT.getOperation());
            throw new InternalGuaranteeException("Ошибка отправки через MongoDb: " +
                    "не удалось отправить событие");
        }
    }
}
