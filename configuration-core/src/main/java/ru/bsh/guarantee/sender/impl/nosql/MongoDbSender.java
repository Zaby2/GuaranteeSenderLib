package ru.bsh.guarantee.sender.impl.nosql;

import com.mongodb.client.MongoClient;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@ConditionalOnProperty(value = "guarantee.nosql.enabled", havingValue = "true")
public class MongoDbSender implements GuaranteeSender {

    private final Map<String, String> dbByClient;
    private final Map<String, MongoClient> mongoClients;
    private final GuaranteeSenderDtoToMongoConverter converter = new GuaranteeSenderDtoToMongoConverter();

    @Override
    public void send(GuaranteeSenderDto dataToSend) {
        var isSend = false;
        for (var name : mongoClients.keySet()) {
            var db = dbByClient.get(name);
            var client = mongoClients.get(name);

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
