package ru.bsh.guarantee.pull.converter;

import org.springframework.core.convert.converter.Converter;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.entity.SqlGuaranteeEntity;

import java.util.Date;

public class GuaranteeSenderEntityToDtoConverter implements
        Converter<SqlGuaranteeEntity, GuaranteeSenderDto> {

    @Override
    public GuaranteeSenderDto convert(SqlGuaranteeEntity source) {
        var result = new GuaranteeSenderDto();
        result.setId(source.getId());
        result.setSignature(source.getSignature());
        result.setRequestType(source.getRequestType());
        result.setRequestValue(source.getRequestValue());
        result.setCreatedAt(source.getCreatedAt() == null ? new Date() : source.getCreatedAt());
        result.setPolledAt(new Date());
        return result;
    }
}
