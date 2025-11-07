package ru.bsh.guarantee.dto;

import lombok.Data;

@Data
public class GuaranteeSenderDto {

    private String signature;
    private String requestValue;
    private String requestType;
}
