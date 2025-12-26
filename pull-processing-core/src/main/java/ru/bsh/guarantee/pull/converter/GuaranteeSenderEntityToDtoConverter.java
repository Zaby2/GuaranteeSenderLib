package ru.bsh.guarantee.pull.converter;

import org.springframework.core.convert.converter.Converter;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.entity.SqlGuaranteeEntity;

public class GuaranteeSenderEntityToDtoConverter implements
        Converter<SqlGuaranteeEntity, GuaranteeSenderDto> {

    @Override
    public GuaranteeSenderDto convert(SqlGuaranteeEntity source) {
        var result = new GuaranteeSenderDto();
        result.setRequestType(source.getRequestType());
        result.setRequestValue(source.getRequestValue());
        result.setCreatedAt(source.getCreatedAt());
        result.setIsSent(source.getIsSent());
        return result;
    }
}
