package com.payservice.paymentservice.controller;

import com.payservice.paymentservice.dto.ReceiptResponseDTO;
import com.payservice.paymentservice.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping("/service/{serviceId}/customer/{customerId}")
    public ResponseEntity<List<ReceiptResponseDTO>> getReceipts(
            @PathVariable Integer serviceId,
            @PathVariable Integer customerId
    ) {
        List<ReceiptResponseDTO> result =
                receiptService.getReceiptsByServiceAndCustomer(serviceId, customerId);

        return ResponseEntity.ok(result);
    }
}
