package ru.bsh.guarantee.converter;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;

import java.util.Date;

public class GuaranteeSenderDtoToMongoConverter implements
        Converter<GuaranteeSenderDto, Document> {

    @Override
    public Document convert(GuaranteeSenderDto source) {
        return new Document()
                .append("signature", source.getSignature())
                .append("requestValue", source.getRequestValue())
                .append("requestType", source.getRequestType())
                .append("createdAt", source.getCreatedAt() == null ? new Date() : source.getCreatedAt())
                .append("polledAt", source.getPolledAt() == null ? new Date() : source.getPolledAt())
                .append("isSent", Boolean.FALSE);
    }
}
