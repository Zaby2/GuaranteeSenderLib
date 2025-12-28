package ru.bsh.guarantee.pull.puller.impl.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.bsh.GuaranteeSenderProxyImpl;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.monitoring.GuaranteeMonitoring;
import ru.bsh.guarantee.pull.puller.PullProcessor;
import service.SignatureService;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;

import static ru.bsh.guarantee.monitoring.MonitoringConstants.BROKER_PULLER;
import static ru.bsh.guarantee.monitoring.MonitoringConstants.SIGNATURE_CHECK;
import static ru.bsh.guarantee.pull.utils.Utils.verifySignature;

@Component
@Slf4j
@ConditionalOnProperty(value = "guarantee.kafka.enabled", havingValue = "true")
public class KafkaPullProcessor implements PullProcessor {

    private final KafkaConsumer<String, GuaranteeSenderDto> kafkaConsumer;
    private final GuaranteeSenderProxyImpl<?> proxy;
    private final SignatureService signatureService;
    private final GuaranteeMonitoring monitoring;

    private final ReentrantLock lock = new ReentrantLock();

    @Value("${guarantee.kafka.puller.durationSec}")
    private Long durationOfSeconds;

    public KafkaPullProcessor(KafkaConsumer<String, GuaranteeSenderDto> kafkaConsumer,
                              GuaranteeSenderProxyImpl<?> proxy,
                              SignatureService signatureService,
                              GuaranteeMonitoring monitoring,
                              @Value("${guarantee.kafka.puller.topic}") String topic) {
        this.kafkaConsumer = kafkaConsumer;
        this.proxy = proxy;
        this.signatureService = signatureService;
        this.monitoring = monitoring;
        this.kafkaConsumer.subscribe(Collections.singletonList(topic));
    }

    @Override
    @Scheduled(cron = "${guarantee.kafka.puller.cron}")
    public void pull() {
        if (!lock.tryLock()) {
            log.debug("KafkaPullProcessor задание было вызвано дважды");
            return;
        }
        try {
            var records = kafkaConsumer.poll(Duration.ofSeconds(durationOfSeconds));

            if (records.isEmpty()) {
                log.info("KafkaPullProcessor не обнаружил не отправленных записей");
            }
            records.forEach(r -> {
                log.info("KafkaPullProcessor обрабатывает запись offset = {}, partition = {}",
                        r.offset(), r.partition());
                var dataToResend = r.value();
                var signature = dataToResend.getSignature();
                dataToResend.setSignature(null);
                if (verifySignature(signatureService, dataToResend, signature)) {
                    log.info("Запись с offset = {}, paratition {} прошла " +
                            "проверку ЭЦП", r.offset(), r.partition());
                    proxy.send(dataToResend);
                } else {
                    monitoring.fail(SIGNATURE_CHECK.getLayer(), SIGNATURE_CHECK.getOperation());
                    log.error("Запись с offset = {}, partition = {}" +
                            " не прошла проверку ЭЦП", r.offset(), r.partition());
                }
            });
            kafkaConsumer.commitSync();

            monitoring.success(BROKER_PULLER.getLayer(), BROKER_PULLER.getOperation());
        } catch (Exception e) {
            monitoring.fail(BROKER_PULLER.getLayer(), BROKER_PULLER.getOperation());
            log.error("Повторная дооотправка записи не удалась {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
