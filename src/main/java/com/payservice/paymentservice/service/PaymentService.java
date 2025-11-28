package com.payservice.paymentservice.service;

import com.payservice.paymentservice.dto.PaymentRequestDTO;
import com.payservice.paymentservice.dto.PaymentResponseDTO;

public interface PaymentService {
    PaymentResponseDTO registerPayment(Integer receiptId, Integer customerId, PaymentRequestDTO request);
}