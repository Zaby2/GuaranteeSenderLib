package ru.bsh.guarantee.converter;

import org.springframework.core.convert.converter.Converter;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.entity.SqlGuaranteeEntity;

public class GuaranteeSenderDtoToEntityConverter implements
        Converter<GuaranteeSenderDto, SqlGuaranteeEntity> {

    @Override
    public SqlGuaranteeEntity convert(GuaranteeSenderDto source) {
        var result = new SqlGuaranteeEntity();
        result.setSignature(source.getSignature());
        result.setRequestType(source.getRequestType());
        result.setRequestValue(source.getRequestValue());
        return result;
    }
}
