package com.payservice.paymentservice.controller;

import com.payservice.paymentservice.dto.ServiceResponseDTO;
import com.payservice.paymentservice.service.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<List<ServiceResponseDTO>> listServicesByCustomer(
            @PathVariable Integer customerId) {

        List<ServiceResponseDTO> result = serviceService.getServicesByCustomer(customerId);
        return ResponseEntity.ok(result);
    }
}
