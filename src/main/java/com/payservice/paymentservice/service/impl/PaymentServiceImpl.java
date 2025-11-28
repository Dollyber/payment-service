package com.payservice.paymentservice.service.impl;

import com.payservice.paymentservice.dto.*;
import com.payservice.paymentservice.entity.*;
import com.payservice.paymentservice.exception.*;
import com.payservice.paymentservice.mapper.PaymentMapper;
import com.payservice.paymentservice.repository.*;
import com.payservice.paymentservice.service.PaymentService;
import com.payservice.paymentservice.util.ExchangeRateConstants;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final ReceiptRepository receiptRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public PaymentResponseDTO registerPayment(Integer receiptId, Integer customerId, PaymentRequestDTO req) {

        //R1: Solo se permite pagar en PEN o USD
        validateCurrency(req.getPaymentCurrency());

        //cargar y validar recibo
        Receipt receipt = loadAndValidateReceipt(receiptId, customerId);

        //RN6: No se puede pagar un recibo nuevo si el anterior no está paga
        validatePendingPreviousReceipts(receipt);

        //Se valida que el monto sea positivo
        BigDecimal amount = validateAmount(req.getAmount());

        //Obtenemos el tipo de cambio
        BigDecimal exchangeRate = determineExchangeRate(
                req.getPaymentCurrency(),
                receipt.getCurrency()
        );

        //RN4: Pagos en moneda distinta están permitidos si no exceden saldo
        BigDecimal amountConverted = convertAmount(amount, req.getPaymentCurrency(),
                receipt.getCurrency(), exchangeRate);


        //RN2: Un servicio puede pagarse parcial o totalmente
        //RN3: Los pagos parciales no pueden exceder el saldo pendiente
        validateNotExceedPending(amountConverted, receipt.getPendingAmount());

        //RN5: El servicio se considera “pagado” cuando el saldo pendiente llega a cero
        validateNotAlreadyPaid(receipt);

        Payment payment = processPayment(receipt, customerId, amount, exchangeRate, amountConverted, req.getPaymentCurrency());

        Customer customer = customerRepository.findById(customerId).orElse(null);
        ServiceEntity service = serviceRepository.findById(receipt.getServiceId()).orElse(null);

        return paymentMapper.toPaymentResponse(payment, customer, service, receipt);
    }

    // VALIDACIONES

    private void validateCurrency(String currency) {
        if (!"PEN".equalsIgnoreCase(currency) && !"USD".equalsIgnoreCase(currency)) {
            throw new BadRequestException("RN1: Only PEN or USD allowed");
        }
    }

    private Receipt loadAndValidateReceipt(Integer receiptId, Integer customerId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found"));

        if (!receipt.getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("Receipt does not belong to customer");
        }
        return receipt;
    }

    private void validatePendingPreviousReceipts(Receipt receipt) {
        List<Receipt> previousReceipts = receiptRepository
                .findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(
                        receipt.getServiceId(), receipt.getCustomerId(), receipt.getDueDate()
                );

        boolean anyUnpaid = previousReceipts
                .stream()
                .anyMatch(r -> !"PAID".equalsIgnoreCase(r.getReceiptStatus()));

        if (anyUnpaid) {
            throw new BadRequestException("RN6: Cannot pay this receipt while previous receipts are unpaid");
        }
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be positive");
        }
        return amount;
    }

    private void validateNotExceedPending(BigDecimal amount, BigDecimal pending) {
        if (amount.compareTo(pending) > 0) {
            throw new BadRequestException("RN3: Payment exceeds pending amount");
        }
    }

    private void validateNotAlreadyPaid(Receipt receipt) {
        if ("PAID".equalsIgnoreCase(receipt.getReceiptStatus())) {
            throw new BadRequestException("RN5: Receipt already PAID; no further payments allowed");
        }
    }

    private BigDecimal convertAmount(BigDecimal amount, String paymentCurrency,
                                     String receiptCurrency, BigDecimal rate) {

        return paymentCurrency.equalsIgnoreCase(receiptCurrency)
                ? amount
                : paymentCurrency.equalsIgnoreCase("USD") ? amount.multiply(rate) : amount.divide(rate, 2, BigDecimal.ROUND_HALF_UP);
    }

    private Payment processPayment(Receipt receipt, Integer customerId,
                                   BigDecimal amount, BigDecimal rate, BigDecimal convertedAmount, String paymentCurrency) {

        BigDecimal previousPending = receipt.getPendingAmount();
        BigDecimal newPending = previousPending.subtract(convertedAmount);

        String newStatus = newPending.compareTo(BigDecimal.ZERO) == 0
                ? "PAID"
                : "PARTIALLY_PAID";

        // update receipt
        receipt.setPendingAmount(newPending);
        receipt.setReceiptStatus(newStatus);
        receiptRepository.save(receipt);

        // create payment
        Payment payment = new Payment();
        payment.setReceiptId(receipt.getReceiptId());
        payment.setCustomerId(customerId);
        payment.setPaymentDate(Instant.now());
        payment.setAmount(amount);
        payment.setPaymentCurrency(paymentCurrency);
        payment.setExchangeRate(rate);
        payment.setPreviousPendingAmount(previousPending);
        payment.setNewPendingAmount(newPending);
        payment.setPaymentStatus(newStatus);

        return paymentRepository.save(payment);
    }

    private BigDecimal determineExchangeRate(String paymentCurrency, String receiptCurrency) {

        if (paymentCurrency == null || receiptCurrency == null) {
            throw new BadRequestException("Currency cannot be null");
        }

        if (paymentCurrency.equalsIgnoreCase(receiptCurrency)) {
            return ExchangeRateConstants.DEFAULT_RATE;
        } else {
            return ExchangeRateConstants.USD_RATE;
        }

        //throw new BadRequestException("Exchange rate not defined for currency " + paymentCurrency);
    }
}

