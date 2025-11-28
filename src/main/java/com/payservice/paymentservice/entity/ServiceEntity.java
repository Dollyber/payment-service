package com.payservice.paymentservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "services", schema = "operation")
@Data @NoArgsConstructor @AllArgsConstructor

public class ServiceEntity {
    @Id
    @Column(name = "service_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer serviceId;

    @Column(name = "customer_id")
    private Integer customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Customer customer;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "date_regist", updatable = false)
    private LocalDateTime dateRegist;

    @Column(name = "user_regist", updatable = false)
    private Integer userRegist;

    @Column(name = "date_modifi")
    private LocalDateTime dateModifi;

    @Column(name = "user_modifi")
    private Integer userModifi;
}
