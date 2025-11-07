package ru.bsh.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;

public class GuaranteeSenderDtoConverter<T> {

    private final ObjectMapper mapper = new ObjectMapper();

    public GuaranteeSenderDto convert(T request) {
        var result = new GuaranteeSenderDto();
        result.setRequestType(request.getClass().getName());
        try {
            result.setRequestValue(mapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
