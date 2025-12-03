package com.payservice.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorResponseDTO {
    private int code;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
}
