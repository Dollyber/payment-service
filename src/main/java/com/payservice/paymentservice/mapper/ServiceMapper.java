package com.payservice.paymentservice.mapper;

import com.payservice.paymentservice.dto.ServiceResponseDTO;
import com.payservice.paymentservice.dto.PaymentResponseDTO;
import com.payservice.paymentservice.dto.ServiceSummaryDTO;
import com.payservice.paymentservice.entity.Customer;
import com.payservice.paymentservice.entity.Payment;
import com.payservice.paymentservice.entity.Receipt;
import com.payservice.paymentservice.entity.ServiceEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ServiceMapper {
    public ServiceSummaryDTO toService(ServiceEntity s) {
        if (s == null) return null;
        return new ServiceSummaryDTO(

                s.getServiceName(),
                s.getDescription()
        );
    }

    public ServiceResponseDTO toServiceResponseDTO(ServiceEntity s, String currency, BigDecimal amount, BigDecimal pendingAmount) {
        if (s == null) return null;
        return new ServiceResponseDTO(
                s.getServiceName(),
                s.getDescription(),
                currency,
                amount,
                pendingAmount
        );
    }

}
