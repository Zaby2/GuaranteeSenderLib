package ru.bsh.guarantee.pull.converter;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;

import java.util.Date;

public class GuaranteeSenderMongoToDtoConverter implements
        Converter<Document, GuaranteeSenderDto> {

    @Override
    public GuaranteeSenderDto convert(Document source) {
        var result = new GuaranteeSenderDto();
        result.setId(Long.valueOf(source.getObjectId("_id").toHexString()));
        result.setSignature(source.getString("signature"));
        result.setRequestValue(source.getString("requestValue"));
        result.setRequestType(source.getString("requestType"));
        result.setCreatedAt(source.getDate("createdAt"));
        result.setPolledAt(new Date());
        return result;
    }
}
