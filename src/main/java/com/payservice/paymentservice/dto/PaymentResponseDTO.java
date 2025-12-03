package com.payservice.paymentservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentResponseDTO {

    //Data Transfer Object para la respuesta del pago
    //Son clases que solo sirven para enviar o recibir datos en la API.
    private CustomerSummaryDTO customer;
    private ServiceSummaryDTO service;
    private ReceiptInfoDTO receipt;

    //private Integer paymentId;
    private BigDecimal amount;
    private String paymentCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal previousPendingAmount;
    private BigDecimal newPendingAmount;
    private String paymentStatus;
    private LocalDateTime paymentDate;
}
