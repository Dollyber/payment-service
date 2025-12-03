package com.payservice.paymentservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReceiptResponseDTO {
    private CustomerSummaryDTO customer;
    private ServiceSummaryDTO service;

    private String receiptNumber;
    private String periodLabel;
    private LocalDate dueDate;
    private BigDecimal receiptAmount;
    private BigDecimal pendingAmount;
    private String currency;
    private String receiptStatus;
}
