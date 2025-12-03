package com.payservice.paymentservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", schema = "operation")
@Data //Genera automáticamente: getters, setters, equals, hashCode y toString
@NoArgsConstructor //Genera constructor vacío
@AllArgsConstructor //Constructor con todos los campos
public class Payment {
    @Id
    @Column(name = "payment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer paymentId;

    @Column(name = "receipt_id")
    private Integer receiptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Receipt receipt;

    @Column(name = "customer_id")
    private Integer customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Customer customer;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be positive")
    private BigDecimal amount;

    @Column(name = "payment_currency")
    private String paymentCurrency;

    @Column(name = "exchange_rate", precision = 12, scale = 2)
    private BigDecimal exchangeRate;

    @Column(name = "previous_pending_amount", precision = 12, scale = 2)
    private BigDecimal previousPendingAmount;

    @Column(name = "new_pending_amount", precision = 12, scale = 2)
    private BigDecimal newPendingAmount;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "date_regist", updatable = false)
    private LocalDateTime dateRegist;

    @Column(name = "user_regist", updatable = false)
    private Integer userRegist;

    @Column(name = "date_modifi", insertable = false)
    private LocalDateTime dateModifi;

    @Column(name = "user_modifi", insertable = false)
    private Integer userModifi;
}
