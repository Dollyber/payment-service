package com.payservice.paymentservice.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers", schema = "operation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    @Column(name = "customer_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer customerId;

    private String names;

    private String lastname;

    private String email;

    @Column(name = "date_regist", updatable = false)
    private LocalDateTime dateRegist;

    @Column(name = "user_regist", updatable = false)
    private Integer userRegist;

    @Column(name = "date_modifi")
    private LocalDateTime dateModifi;

    @Column(name = "user_modifi")
    private Integer userModifi;

}
