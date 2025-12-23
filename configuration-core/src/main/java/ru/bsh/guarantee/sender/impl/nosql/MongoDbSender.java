package ru.bsh.guarantee.sender.impl.nosql;

import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.bsh.guarantee.converter.GuaranteeSenderDtoToMongoConverter;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.exception.InternalGuaranteeException;
import ru.bsh.guarantee.sender.GuaranteeSender;

import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@ConditionalOnProperty(value = "guarantee.nosql.enabled", havingValue = "true")
public class MongoDbSender implements GuaranteeSender {

    private final Map<String, MongoClient> mongoClients;

    private final GuaranteeSenderDtoToMongoConverter converter = new GuaranteeSenderDtoToMongoConverter();

    public MongoDbSender(Map<String, MongoClient> mongoClients) {
        this.mongoClients = mongoClients;
    }

    @Override
    public void send(GuaranteeSenderDto dataToSend) {
        var isSend = false;
        for (var name : mongoClients.keySet()) {
            MongoClient client;
            try {
                client = mongoClients.get(name);
            } catch (Exception e) {
                log.error("При попытке переключения между Монго " +
                        "транспортами возникла ошибка {}, веротяна ошибка конфигурации", e.getMessage());
                continue;
            }

            try {
                client.getDatabase(name)
                        .getCollection("guarantee_data")
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
