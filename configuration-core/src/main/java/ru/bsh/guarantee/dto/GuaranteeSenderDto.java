package ru.bsh.guarantee.dto;

import lombok.Data;

import java.util.Date;

@Data
public class GuaranteeSenderDto {

    private String signature;
    private String requestValue;
    private String requestType;
    private Date createdAt;
    private Date polledAt;
    private Boolean isSent;
}
