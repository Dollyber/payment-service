package com.payservice.paymentservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "receipts", schema = "operation")
@Data @NoArgsConstructor @AllArgsConstructor

public class Receipt {
    @Id
    @Column(name = "receipt_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer receiptId;

    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "service_id")
    private Integer serviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ServiceEntity service;

    @Column(name = "customer_id")
    private Integer customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Customer customer;

    @Column(name = "period_label")
    private String periodLabel;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "receipt_amount", precision = 12, scale = 2)
    private BigDecimal receiptAmount;

    private String currency;

    @Column(name = "pending_amount", precision = 12, scale = 2)
    private BigDecimal pendingAmount;

    @Column(name = "receipt_status")
    private String receiptStatus;
}
