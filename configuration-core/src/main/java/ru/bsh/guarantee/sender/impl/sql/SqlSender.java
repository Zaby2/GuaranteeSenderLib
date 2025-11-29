package ru.bsh.guarantee.sender.impl.sql;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.bsh.guarantee.converter.GuaranteeSenderDtoToEntityConverter;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.exception.InternalGuaranteeException;
import ru.bsh.guarantee.repository.GuaranteeJpaRepository;
import ru.bsh.guarantee.sender.GuaranteeSender;
import ru.bsh.guarantee.sender.configuration.sql.DbContext;
import ru.bsh.guarantee.sender.configuration.sql.SqlSenderConfiguration;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@ConditionalOnProperty(name = "guarantee.sql.enabled", havingValue = "true")
public class SqlSender implements GuaranteeSender {

    private final List<String> destinations;
    private final GuaranteeJpaRepository repository;
    private final GuaranteeSenderDtoToEntityConverter converter = new GuaranteeSenderDtoToEntityConverter();

    public SqlSender(SqlSenderConfiguration configuration, GuaranteeJpaRepository repository) {
        this.destinations = configuration.getProperties().getPropertiesMap().keySet().stream().toList();
        log.info("Инициализирован Sql sender, точки оптравки {}", destinations);
        this.repository = repository;
    }

    @Override
    public void send(GuaranteeSenderDto dataToSend) {
        var isSend = false;
        for (var destination : destinations) {
            DbContext.set(destination);
            try {
                repository.save(Objects.requireNonNull(converter.convert(dataToSend)));
                isSend = true;
                log.info("Отпарвка в SQL БД {} завершилась успешно", DbContext.get());
                break;
            } catch (Exception e) {
                log.error("Ошибка отправки в SQL БД {}, БД {}", e.getMessage(), DbContext.get());
            }
        }
        if (!isSend) {
            DbContext.clear();
            throw new InternalGuaranteeException("Ошибка отправки через SQL транспорт");
        }
    }
}
