package com.payservice.paymentservice.service.impl;

import com.payservice.paymentservice.dto.*;
import com.payservice.paymentservice.entity.*;
import com.payservice.paymentservice.mapper.PaymentMapper;
import com.payservice.paymentservice.repository.*;
import com.payservice.paymentservice.service.PaymentService;
import com.payservice.paymentservice.util.ExchangeRateConstants;
import com.payservice.paymentservice.util.exception.NoPaymentsFoundException;
import com.payservice.paymentservice.util.exception.OverpaymentException;
import com.payservice.paymentservice.util.exception.PendingReceiptException;
import com.payservice.paymentservice.util.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

        //RN1: Solo se permite pagar en PEN o USD
        validateCurrency(req.getPaymentCurrency());

        //cargar y validar recibo
        Receipt receipt = loadAndValidateReceipt(receiptId, customerId);

        //RN5: El servicio se considera “pagado” cuando el saldo pendiente llega a cero
        validateNotAlreadyPaid(receipt);

        //RN6: No se puede pagar un recibo nuevo si el anterior no está pagado
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

        Payment payment = processPayment(receipt, customerId, amount, exchangeRate, amountConverted, req.getPaymentCurrency());

        Customer customer = customerRepository.findById(customerId).orElse(null);
        ServiceEntity service = serviceRepository.findById(receipt.getServiceId()).orElse(null);

        return paymentMapper.toPaymentResponse(payment, customer, service, receipt);
    }

    // VALIDACIONES
    private void validateCurrency(String currency) {
        if (currency == null) throw new IllegalArgumentException("Currency cannot be null");
        if (!"PEN".equalsIgnoreCase(currency) && !"USD".equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("RN1: Only PEN or USD allowed"); //Advise Controller
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
            throw new PendingReceiptException("RN6: Cannot pay this receipt while previous receipts are unpaid");
        }
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return amount;
    }

    private void validateNotAlreadyPaid(Receipt receipt) {
        if ("PAID".equalsIgnoreCase(receipt.getReceiptStatus())) {
            throw new IllegalArgumentException("RN5: Receipt already PAID; no further payments allowed");
        }
    }

    private void validateNotExceedPending(BigDecimal amount, BigDecimal pending) {
        if (amount.compareTo(pending) > 0) {
            throw new OverpaymentException("RN3: Payment exceeds pending amount");
        }
    }

    private BigDecimal determineExchangeRate(String paymentCurrency, String receiptCurrency) {

        if (paymentCurrency == null || receiptCurrency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }

        if (paymentCurrency.equalsIgnoreCase(receiptCurrency)) {
            return ExchangeRateConstants.DEFAULT_RATE;
        } else {
            return ExchangeRateConstants.USD_RATE;
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
        receipt.setUserModifi(1); //System user
        receipt.setDateModifi(LocalDateTime.now());
        receiptRepository.save(receipt);

        // create payment
        Payment payment = new Payment();
        payment.setReceiptId(receipt.getReceiptId());
        payment.setCustomerId(customerId);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setAmount(amount);
        payment.setPaymentCurrency(paymentCurrency);
        payment.setExchangeRate(rate);
        payment.setPreviousPendingAmount(previousPending);
        payment.setNewPendingAmount(newPending);
        payment.setPaymentStatus(newStatus);
        payment.setDateRegist(LocalDateTime.now());
        payment.setUserRegist(1); //System user

        return paymentRepository.save(payment);
    }

    //Api historial de pago
    @Override
    public List<PaymentResponseDTO> getPaymentsByCustomer(Integer customerId) {

        // Validar si el cliente existe
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Obtener pagos
        List<Payment> payments = paymentRepository
                .findByCustomerIdOrderByPaymentDateDesc(customerId);

        if (payments.isEmpty()) {
            throw new NoPaymentsFoundException("Customer has no registered payments");
        }

        // Mapear todos los pagos a DTO
        return payments.stream().map(payment -> {

            // cargar recibo y servicio relacionados
            Receipt receipt = receiptRepository.findById(payment.getReceiptId())
                    .orElse(null);

            ServiceEntity service = receipt != null ?
                    serviceRepository.findById(receipt.getServiceId()).orElse(null)
                    : null;

            Customer customer = customerRepository.findById(customerId).orElse(null);

            return paymentMapper.toPaymentResponse(payment, customer, service, receipt);

        }).toList();
    }

}

