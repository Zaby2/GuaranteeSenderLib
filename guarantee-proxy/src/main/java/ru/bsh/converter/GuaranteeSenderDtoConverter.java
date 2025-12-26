package ru.bsh.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.exception.InternalGuaranteeException;

import java.util.Date;

public class GuaranteeSenderDtoConverter<T> {

    private final ObjectMapper mapper = new ObjectMapper();

    public GuaranteeSenderDto convert(T request) {
        var result = new GuaranteeSenderDto();
        result.setRequestType(request.getClass().getName());
        try {
            result.setRequestValue(mapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new InternalGuaranteeException(e.getMessage());
        }
        result.setCreatedAt(new Date());
        result.setIsSent(Boolean.FALSE);
        return result;
    }
}
