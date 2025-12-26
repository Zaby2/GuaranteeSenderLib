package ru.bsh.guarantee.pull.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.exception.InternalGuaranteeException;
import service.SignatureService;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {

    public static Boolean verifySignature(SignatureService signatureService,
                                          GuaranteeSenderDto dto,
                                          String signature) {
        var objectMapper = new ObjectMapper();
        String stringData;
        try {
            stringData = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new InternalGuaranteeException(
                    String.format("Ошибка преобразования в строку при подписании %s", e.getMessage())
            );
        }
        try {
            return signatureService.verify(stringData, signature);
        } catch (Exception e) {
            throw new InternalGuaranteeException(e.getMessage());
        }
    }

    public static Integer determineCurrentIndex(Integer index, Integer size) {
        if (index >= size - 1) {
            return 0;
        } else {
            return 1;
        }
    }
}
