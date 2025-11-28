package com.payservice.paymentservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class PaymentRequestDTO {
    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be positive")
    private BigDecimal amount;

    @NotBlank
    private String paymentCurrency;
}
