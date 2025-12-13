package ru.bsh.guarantee.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Entity
@Data
public class SqlGuaranteeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String signature;
    private String requestValue;
    private String requestType;
    private Date createdAt;
    private Date polledAt;
    private Boolean isSent;
}
