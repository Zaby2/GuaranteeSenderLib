package ru.bsh.guarantee.sender.impl.sql;

import lombok.extern.slf4j.Slf4j;
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
        try {
            repository.save(Objects.requireNonNull(converter.convert(dataToSend)));
            log.info("Отпарвка в SQL БД {} завершилась успешно", DbContext.get());
        } catch (Exception e) {
            throw new InternalGuaranteeException(String.format("Ошибка отправки в SQL БД %s", e.getMessage()));
        }
    }
}
