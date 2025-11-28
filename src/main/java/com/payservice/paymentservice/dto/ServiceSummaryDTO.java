package com.payservice.paymentservice.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ServiceSummaryDTO {
    //private Integer serviceId;
    private String serviceName;
    private Boolean isActive;
    private String description;
}

