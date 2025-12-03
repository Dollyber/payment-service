package com.payservice.paymentservice.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CustomerSummaryDTO {
    private String names;
    private String lastname;
    private String email;
}
