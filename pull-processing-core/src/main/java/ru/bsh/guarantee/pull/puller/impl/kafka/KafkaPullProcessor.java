package ru.bsh.guarantee.pull.puller.impl.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.bsh.GuaranteeSenderProxyImpl;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.pull.puller.PullProcessor;
import service.SignatureService;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;

import static ru.bsh.guarantee.pull.utils.Utils.verifySignature;

@Component
@Slf4j
@ConditionalOnProperty(value = "guarantee.kafka.enabled", havingValue = "true")
public class KafkaPullProcessor implements PullProcessor {

    @Value("${kafka.puller.topic}")
    private String topic;

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
        this.kafkaConsumer.subscribe(Collections.singletonList(topic));
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
                if (verifySignature(signatureService, dataToResend)) {
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
}
