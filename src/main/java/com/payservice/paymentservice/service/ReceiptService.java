package com.payservice.paymentservice.service;

import com.payservice.paymentservice.dto.ReceiptResponseDTO;

import java.util.List;

public interface ReceiptService {
    List<ReceiptResponseDTO> getReceiptsByServiceAndCustomer(Integer serviceId, Integer customerId);
}
