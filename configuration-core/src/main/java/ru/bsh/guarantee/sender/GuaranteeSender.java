package ru.bsh.guarantee.sender;

import ru.bsh.guarantee.dto.GuaranteeSenderDto;

public interface GuaranteeSender {

    void send(GuaranteeSenderDto dataToSend);
}
