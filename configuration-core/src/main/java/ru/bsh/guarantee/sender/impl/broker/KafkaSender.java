package ru.bsh.guarantee.sender.impl.broker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.exception.InternalGuaranteeException;
import ru.bsh.guarantee.sender.GuaranteeSender;

import java.util.concurrent.ExecutionException;

import static ru.bsh.guarantee.utils.KafkaUtils.getExceptionCause;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "guarantee.kafka.enabled", havingValue = "true")
public class KafkaSender implements GuaranteeSender {

    //@Value
    private final String topic;
    private final KafkaTemplate<String, GuaranteeSenderDto> kafkaTemplate;

    @Override
    public void send(GuaranteeSenderDto dataToSend) {
        try {
            kafkaTemplate.send(topic, dataToSend).get();
            log.info("Успешная отправка через kafka транспорт");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalGuaranteeException(
                    String.format("Внутренняя ошибка прерывания потока %s", e.getMessage()));
        } catch (ExecutionException e) {
            var cause = getExceptionCause(e); // Для метрики
            throw new InternalGuaranteeException(
                    String.format("Ошибка отправки через kafka транспорт %s", cause.getMessage())
            );
        }
    }
}
