package ru.bsh.guarantee.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Table(name = "guarantee_data")
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
