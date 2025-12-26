package ru.bsh.guarantee.pull.converter;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;

public class GuaranteeSenderMongoToDtoConverter implements
        Converter<Document, GuaranteeSenderDto> {

    @Override
    public GuaranteeSenderDto convert(Document source) {
        var result = new GuaranteeSenderDto();
        result.setRequestValue(source.getString("requestValue"));
        result.setRequestType(source.getString("requestType"));
        result.setCreatedAt(source.getDate("createdAt"));
        return result;
    }
}
