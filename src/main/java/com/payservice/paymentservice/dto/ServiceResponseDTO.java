package com.payservice.paymentservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceResponseDTO {
    private String serviceName;
    private String description;
    private String currency;
    private BigDecimal amount;
    private BigDecimal pendingAmount;
}

