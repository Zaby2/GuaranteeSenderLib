package ru.bsh.guarantee.pull.puller.impl.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.bsh.GuaranteeSenderProxyImpl;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.exception.InternalGuaranteeException;
import ru.bsh.guarantee.pull.puller.PullProcessor;
import service.SignatureService;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@ConditionalOnProperty(value = "guarantee.kafka.enabled", havingValue = "true")
public class KafkaPullProcessor implements PullProcessor {

    private String topic; // todo @Value

    private final KafkaConsumer<String, GuaranteeSenderDto> kafkaConsumer;
    private final GuaranteeSenderProxyImpl<?> proxy;
    private final SignatureService signatureService;
    private final ReentrantLock lock = new ReentrantLock();

    public KafkaPullProcessor(KafkaConsumer<String, GuaranteeSenderDto> kafkaConsumer,
                              GuaranteeSenderProxyImpl<?> proxy,
                              SignatureService signatureService) {
        this.kafkaConsumer = kafkaConsumer;
        this.proxy = proxy;
        this.signatureService = signatureService;
        kafkaConsumer.subscribe(Collections.singletonList(topic));
    }

    @Override
    @Scheduled(cron = "${kafka.pull.cron}")
    public void pull() {
        if (!lock.tryLock()) {
            log.debug("KafkaPullProcessor задание было вызвано дважды");
            return;
        }
        try {
            var records = kafkaConsumer.poll(Duration.ofSeconds(1));
            if (records.isEmpty()) {
                log.info("KafkaPullProcessor не обнаружил не отправленных записей");
            }
            records.forEach(r -> {
                log.info("KafkaPullProcessor обрабатывает запись с key = {}", r.key());
                var dataToResend = r.value();
                if (verifySignature(dataToResend)) {
                    log.info("Запись с key = {} прошла проверку ЭЦП", r.key());
                    proxy.send(dataToResend);
                } else {
                    log.error("Запись с key = {} не прошла проверку ЭЦП", r.key());
                }
            });
            kafkaConsumer.commitSync();
        } catch (Exception e) {
            log.error("Повторная дооотправка записи не удалась, проверьте доступность транспортов");
        } finally {
            lock.unlock();
        }
    }

    private Boolean verifySignature(GuaranteeSenderDto dto) {
        var objectMapper = new ObjectMapper();
        String stringData;
        try {
            stringData = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new InternalGuaranteeException(
                    String.format("Ошибка преобразования в строку при подписании %s", e.getMessage())
            );
        }
        try {
            return signatureService.verify(stringData, dto.getSignature());
        } catch (Exception e) {
            throw new InternalGuaranteeException(e.getMessage());
        }
    }
}
