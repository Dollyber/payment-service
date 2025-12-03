package com.payservice.paymentservice.mapper;

import com.payservice.paymentservice.dto.PaymentResponseDTO;
import com.payservice.paymentservice.entity.Customer;
import com.payservice.paymentservice.entity.Payment;
import com.payservice.paymentservice.entity.Receipt;
import com.payservice.paymentservice.entity.ServiceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    @Autowired
    private CustomerMapper customerMapper;
    @Autowired
    private ServiceMapper serviceMapper;
    @Autowired
    private ReceiptMapper receiptMapper;

    public PaymentResponseDTO toPaymentResponse(Payment p, Customer c, ServiceEntity s, Receipt r) {
        return new PaymentResponseDTO(
                customerMapper.toCustomer(c),
                serviceMapper.toService(s),
                receiptMapper.toReceiptInfo(r),
                p.getAmount(),
                p.getPaymentCurrency(),
                p.getExchangeRate(),
                p.getPreviousPendingAmount(),
                p.getNewPendingAmount(),
                p.getPaymentStatus(),
                p.getPaymentDate()
        );
    }

}
