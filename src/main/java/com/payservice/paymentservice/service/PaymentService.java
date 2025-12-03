package com.payservice.paymentservice.service;

import com.payservice.paymentservice.dto.PaymentRequestDTO;
import com.payservice.paymentservice.dto.PaymentResponseDTO;
import java.util.List;

public interface PaymentService {
    PaymentResponseDTO registerPayment(Integer receiptId, Integer customerId, PaymentRequestDTO request);
    List<PaymentResponseDTO> getPaymentsByCustomer(Integer customerId);
}

