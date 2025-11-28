package com.payservice.paymentservice.mapper;

import com.payservice.paymentservice.dto.ReceiptInfoDTO;
import com.payservice.paymentservice.entity.Receipt;
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

}
