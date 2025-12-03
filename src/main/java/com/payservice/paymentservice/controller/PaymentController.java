package com.payservice.paymentservice.controller;

import com.payservice.paymentservice.dto.*;
import com.payservice.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/receipts/{receiptId}/customer/{customerId}")
    public ResponseEntity<PaymentResponseDTO> registerPayment(
            @PathVariable Integer receiptId,
            @PathVariable Integer customerId,
            @Valid @RequestBody PaymentRequestDTO request) {

        PaymentResponseDTO resp = paymentService.registerPayment(receiptId, customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByCustomer(
            @PathVariable Integer customerId) {

        List<PaymentResponseDTO> list = paymentService.getPaymentsByCustomer(customerId);
        return ResponseEntity.ok(list);
    }

}
