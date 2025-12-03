package com.payservice.paymentservice.service.impl;

import com.payservice.paymentservice.dto.ReceiptResponseDTO;
import com.payservice.paymentservice.entity.Customer;
import com.payservice.paymentservice.entity.Receipt;
import com.payservice.paymentservice.entity.ServiceEntity;
import com.payservice.paymentservice.mapper.ReceiptMapper;
import com.payservice.paymentservice.repository.CustomerRepository;
import com.payservice.paymentservice.repository.PaymentRepository;
import com.payservice.paymentservice.repository.ReceiptRepository;
import com.payservice.paymentservice.repository.ServiceRepository;
import com.payservice.paymentservice.service.ReceiptService;
import com.payservice.paymentservice.util.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiptServiceImpl implements ReceiptService {
    private final ReceiptMapper receiptMapper;
    private final ReceiptRepository receiptRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public List<ReceiptResponseDTO> getReceiptsByServiceAndCustomer(Integer serviceId, Integer customerId) {

        // validar que el customer exista
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // validar que el service exista
        serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        // obtener recibos
        List<Receipt> receipts = receiptRepository
                .findByServiceIdAndCustomerIdOrderByDueDateDesc(serviceId, customerId);

        if (receipts.isEmpty()) {
            throw new ResourceNotFoundException("No receipts found for this customer/service");
        }

        // obtener customer y service para mapear
        Customer customer = customerRepository.findById(customerId).orElse(null);
        ServiceEntity service = serviceRepository.findById(serviceId).orElse(null);

        // construir respuesta
        return receipts.stream()
                .map(r -> receiptMapper.toFullReceiptInfo(r, customer, service))
                .toList();
    }

}
