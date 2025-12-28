package ru.bsh.guarantee.sender.impl.sql;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.bsh.guarantee.converter.GuaranteeSenderDtoToEntityConverter;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.exception.InternalGuaranteeException;
import ru.bsh.guarantee.monitoring.GuaranteeMonitoring;
import ru.bsh.guarantee.repository.GuaranteeJpaRepository;
import ru.bsh.guarantee.sender.GuaranteeSender;
import ru.bsh.guarantee.sender.configuration.sql.DbContext;
import ru.bsh.guarantee.sender.configuration.sql.SqlSenderConfiguration;

import java.util.List;
import java.util.Objects;

import static ru.bsh.guarantee.monitoring.MonitoringConstants.*;

@Slf4j
@Service
@ConditionalOnProperty(name = "guarantee.sql.enabled", havingValue = "true")
public class SqlSender implements GuaranteeSender {

    private final List<String> destinations;
    private final GuaranteeJpaRepository repository;
    private final GuaranteeMonitoring monitoring;
    private final GuaranteeSenderDtoToEntityConverter converter = new GuaranteeSenderDtoToEntityConverter();

    public SqlSender(SqlSenderConfiguration configuration,
                     GuaranteeJpaRepository repository,
                     GuaranteeMonitoring monitoring) {
        this.destinations = configuration.getProperties().getPropertiesMap().keySet().stream().toList();
        this.monitoring = monitoring;
        this.repository = repository;
        log.info("Инициализирован Sql sender, точки оптравки {}", destinations);
    }

    @Override
    public void send(GuaranteeSenderDto dataToSend) {
        var isSend = false;
        for (var destination : destinations) {
            DbContext.set(destination);
            try {
                repository.save(Objects.requireNonNull(converter.convert(dataToSend)));
                isSend = true;
                monitoring.success(SQL_SENDER.getLayer(), SQL_SENDER.getOperation());
                log.info("Отпарвка в SQL БД {} завершилась успешно", DbContext.get());
                break;
            } catch (Exception e) {
                monitoring.fail(SQL_SENDER.getLayer(), SQL_SENDER.getOperation());
                log.error("Ошибка отправки в SQL БД {}, БД {}", e.getMessage(), DbContext.get());
            }
        }
        if (!isSend) {
            monitoring.fail(TRANSPORT.getLayer(), TRANSPORT.getOperation());
            DbContext.clear();
            throw new InternalGuaranteeException("Ошибка отправки через SQL транспорт:" +
                    " не удалось отправить событие");
        }
    }
}
