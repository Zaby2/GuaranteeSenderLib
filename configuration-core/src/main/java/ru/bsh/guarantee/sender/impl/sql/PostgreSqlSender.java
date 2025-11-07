package ru.bsh.guarantee.sender.impl.sql;

import lombok.RequiredArgsConstructor;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.sender.GuaranteeSender;
import ru.bsh.guarantee.sender.configuration.DbSenderConfiguration;

@RequiredArgsConstructor
public class PostgreSqlSender implements GuaranteeSender {

    private final DbSenderConfiguration configuration;

    @Override
    public void send(GuaranteeSenderDto dataToSend) {

    }
}
