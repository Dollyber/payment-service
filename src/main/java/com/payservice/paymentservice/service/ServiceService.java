package com.payservice.paymentservice.service;

import com.payservice.paymentservice.dto.ServiceResponseDTO;

import java.util.List;

public interface ServiceService {
    List<ServiceResponseDTO> getServicesByCustomer(Integer customerId);
}
