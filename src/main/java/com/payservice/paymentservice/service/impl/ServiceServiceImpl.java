package com.payservice.paymentservice.service.impl;

import com.payservice.paymentservice.dto.ServiceResponseDTO;
import com.payservice.paymentservice.dto.ServiceSummaryDTO;
import com.payservice.paymentservice.entity.Customer;
import com.payservice.paymentservice.entity.Receipt;
import com.payservice.paymentservice.entity.ServiceEntity;
import com.payservice.paymentservice.mapper.ServiceMapper;
import com.payservice.paymentservice.repository.CustomerRepository;
import com.payservice.paymentservice.repository.ReceiptRepository;
import com.payservice.paymentservice.repository.ServiceRepository;
import com.payservice.paymentservice.service.ReceiptService;
import com.payservice.paymentservice.service.ServiceService;
import com.payservice.paymentservice.util.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceServiceImpl implements ServiceService {

    private final ServiceMapper serviceMapper;
    private final ReceiptRepository receiptRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;

    @Override
    public List<ServiceResponseDTO> getServicesByCustomer(Integer customerId) {

        // Validar cliente
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Buscar servicios asociados
        List<ServiceEntity> services = serviceRepository.findByCustomerId(customerId);

        if (services.isEmpty()) {
            throw new ResourceNotFoundException("Customer has no registered services");
        }

        // Procesar cada servicio
        return services.stream().map(service -> {

            List<Receipt> receipts = receiptRepository
                    .findByServiceIdAndCustomerId(service.getServiceId(), customerId);

            // Sin recibos para este servicio
            if (receipts.isEmpty()) {
                throw new ResourceNotFoundException(
                        "Service " + service.getServiceName() + " has no receipts for this customer"
                );
            }

            // Calcular moneda
            Set<String> currencies = receipts.stream()
                    .map(Receipt::getCurrency)
                    .collect(Collectors.toSet());

            String currency = (currencies.size() == 1)
                    ? currencies.iterator().next()
                    : "MULTIMONEDA";

            // 3.3 → Calcular montos
            BigDecimal totalAmount = receipts.stream()
                    .map(Receipt::getReceiptAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPending = receipts.stream()
                    .map(Receipt::getPendingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Enviar datos
            return serviceMapper.toServiceResponseDTO(
                    service,
                    currency,
                    totalAmount,
                    totalPending
            );

        }).toList();
    }
}
