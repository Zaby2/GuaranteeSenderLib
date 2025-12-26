package ru.bsh.guarantee.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@Table(name = "guarantee_data")
public class SqlGuaranteeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "signature")
    private byte[] signature;
    private String requestValue;
    private String requestType;
    private Date createdAt;
    private Date polledAt;
    private Boolean isSent;
}
