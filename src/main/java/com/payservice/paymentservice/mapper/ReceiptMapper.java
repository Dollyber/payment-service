package com.payservice.paymentservice.mapper;

import com.payservice.paymentservice.dto.CustomerSummaryDTO;
import com.payservice.paymentservice.dto.ReceiptInfoDTO;
import com.payservice.paymentservice.dto.ReceiptResponseDTO;
import com.payservice.paymentservice.dto.ServiceSummaryDTO;
import com.payservice.paymentservice.entity.Customer;
import com.payservice.paymentservice.entity.Receipt;
import com.payservice.paymentservice.entity.ServiceEntity;
import org.springframework.stereotype.Component;

@Component
public class ReceiptMapper {
    public ReceiptInfoDTO toReceiptInfo(Receipt r) {
        if (r == null) return null;
        return new ReceiptInfoDTO(

                r.getReceiptNumber(),
                r.getPeriodLabel(),
                r.getDueDate(),
                r.getReceiptAmount(),
                r.getPendingAmount(),
                r.getCurrency(),
                r.getReceiptStatus()
        );
    }

    public ReceiptResponseDTO toFullReceiptInfo(
            Receipt r,
            Customer c,
            ServiceEntity s
    ) {
        return ReceiptResponseDTO.builder()
                .customer(
                        c == null ? null :
                                new CustomerSummaryDTO(c.getNames(), c.getLastname(), c.getEmail())
                )
                .service(
                        s == null ? null :
                                new ServiceSummaryDTO(s.getServiceName(), s.getDescription())
                )
                .receiptNumber(r.getReceiptNumber())
                .periodLabel(r.getPeriodLabel())
                .dueDate(r.getDueDate())
                .receiptAmount(r.getReceiptAmount())
                .pendingAmount(r.getPendingAmount())
                .currency(r.getCurrency())
                .receiptStatus(r.getReceiptStatus())
                .build();
    }

}
