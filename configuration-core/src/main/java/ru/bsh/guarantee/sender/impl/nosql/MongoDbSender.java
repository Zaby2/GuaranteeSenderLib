package ru.bsh.guarantee.sender.impl.nosql;

import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.bsh.guarantee.converter.GuaranteeSenderDtoToMongoConverter;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.exception.InternalGuaranteeException;
import ru.bsh.guarantee.sender.GuaranteeSender;
import ru.bsh.guarantee.sender.configuration.nosql.MongoConfiguration;

import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@ConditionalOnProperty(value = "guarantee.nosql.enabled", havingValue = "true")
public class MongoDbSender implements GuaranteeSender {

    private final MongoConfiguration configuration;
    private final Map<String, MongoClient> mongoClients;
    private final GuaranteeSenderDtoToMongoConverter converter = new GuaranteeSenderDtoToMongoConverter();

    public MongoDbSender(MongoConfiguration configuration, Map<String, MongoClient> mongoClients) {
        this.configuration = configuration;
        this.mongoClients = mongoClients;
    }

    @Override
    public void send(GuaranteeSenderDto dataToSend) {
        var isSend = false;
        var dbByClients = configuration.getMongoSenderDto().getConnections();
        for (var name : mongoClients.keySet()) {
            String db;
            MongoClient client;
            try {
                db = dbByClients.get(name);
                client = mongoClients.get(name);
            } catch (Exception e) {
                log.error("При попытке переключения между Монго " +
                        "транспортами возникла ошибка {}, веротяна ошибка конфигурации", e.getMessage());
                continue;
            }

            try {
                client.getDatabase(db)
                        .getCollection("guarantee")
                        .insertOne(Objects.requireNonNull(converter.convert(dataToSend)));
                log.info("Отправка через MongoDb {} транспорт произошла успешно", name);
                isSend = true;
                break;
            } catch (Exception e) {
                log.error("Отправка через MongoDb {} транспорт произошла с ошибкой {}", name, e.getMessage());
            }
        }
        if (!isSend) {
            throw new InternalGuaranteeException("Ошибка отправки через MongoDb");
        }
    }
}
