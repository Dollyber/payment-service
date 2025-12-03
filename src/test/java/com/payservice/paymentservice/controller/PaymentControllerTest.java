package com.payservice.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payservice.paymentservice.dto.*;
import com.payservice.paymentservice.service.PaymentService;
import com.payservice.paymentservice.util.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentControllerTest {

    private MockMvc mockMvc; //probar controladores REST sin necesidad de levantar un servidor web
    private PaymentService paymentService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        //Crea una versión falsa del service
        paymentService = Mockito.mock(PaymentService.class);

        //standaloneSetup: Crea un MockMvc solo con el Controller
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper(); //convertir RequestDTO a JSON.
    }

    @Test
    void registerPayment_ShouldReturn201_WhenSuccessful() throws Exception {
        // Request DTO
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("50.00"));
        request.setPaymentCurrency("PEN");

        // Response DTO (lo que el servicio devolvería)
        CustomerSummaryDTO customerDTO = new CustomerSummaryDTO(
                "Dolly", "Asto", "dolly@mail.com"
        );

        ServiceSummaryDTO serviceDTO = new ServiceSummaryDTO(
                "Internet Hogar", "Plan mensual"
        );

        ReceiptInfoDTO receiptDTO = new ReceiptInfoDTO(
                "00000010",
                "ENE-2024",
                LocalDate.now(),
                new BigDecimal("150.00"),
                new BigDecimal("50.00"),
                "PEN",
                "PENDING"
        );

        PaymentResponseDTO response = PaymentResponseDTO.builder()
                .customer(customerDTO)
                .service(serviceDTO)
                .receipt(receiptDTO)
                .amount(new BigDecimal("50.00"))
                .paymentCurrency("PEN")
                .exchangeRate(new BigDecimal("1.00"))
                .paymentStatus("PAID")
                .paymentDate(LocalDateTime.now())
                .build();

        // Mockear la llamada al servicio
        Mockito.when(paymentService.registerPayment(eq(10), eq(5), any(PaymentRequestDTO.class)))
                .thenReturn(response);

        // Ejecutar el POST con MockMvc
        mockMvc.perform(post("/payments/receipts/10/customer/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customer.names").value("Dolly"))
                .andExpect(jsonPath("$.customer.lastname").value("Asto"))
                .andExpect(jsonPath("$.customer.email").value("dolly@mail.com"))
                .andExpect(jsonPath("$.service.serviceName").value("Internet Hogar"))
                .andExpect(jsonPath("$.service.description").value("Plan mensual"))
                .andExpect(jsonPath("$.receipt.receiptNumber").value("00000010"))
                .andExpect(jsonPath("$.receipt.currency").value("PEN"))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.paymentCurrency").value("PEN"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    void registerPayment_ShouldReturn422_WhenOverpayment() throws Exception {

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("1000.0"));
        request.setPaymentCurrency("PEN");

        Mockito.when(paymentService.registerPayment(eq(10), eq(5), any()))
                .thenThrow(new OverpaymentException("RN3: Payment exceeds pending amount"));

        mockMvc.perform(post("/payments/receipts/10/customer/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("RN3: Payment exceeds pending amount"))
                .andExpect(jsonPath("$.path").value("/payments/receipts/10/customer/5"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void registerPayment_ShouldReturn409_WhenPendingReceipt() throws Exception {
        // Arrange:
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("50.0"));
        request.setPaymentCurrency("PEN");

        Mockito.when(paymentService.registerPayment(eq(10), eq(5), any()))
                .thenThrow(new PendingReceiptException("RN6: Cannot pay this receipt while previous receipts are unpaid"));

        mockMvc.perform(post("/payments/receipts/10/customer/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("RN6: Cannot pay this receipt while previous receipts are unpaid"))
                .andExpect(jsonPath("$.path").value("/payments/receipts/10/customer/5"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void registerPayment_ShouldReturn404_WhenNotFound() throws Exception {
        // Arrange:
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("50.0"));
        request.setPaymentCurrency("PEN");

        Mockito.when(paymentService.registerPayment(eq(10), eq(5), any()))
                .thenThrow(new ResourceNotFoundException("Receipt not found"));

        mockMvc.perform(post("/payments/receipts/10/customer/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Receipt not found"))
                .andExpect(jsonPath("$.path").value("/payments/receipts/10/customer/5"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void registerPayment_ShouldReturn400_WhenInvalidCurrency() throws Exception {
        // Arrange:
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("50.0"));
        request.setPaymentCurrency("XYZ"); // inválido

        Mockito.when(paymentService.registerPayment(eq(10), eq(5), any()))
                .thenThrow(new IllegalArgumentException("RN1: Only PEN or USD allowed"));

        mockMvc.perform(post("/payments/receipts/10/customer/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("RN1: Only PEN or USD allowed"))
                .andExpect(jsonPath("$.path").value("/payments/receipts/10/customer/5"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void registerPayment_ShouldReturn500_WhenUnexpectedError() throws Exception {
        // Arrange:
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("30.0"));
        request.setPaymentCurrency("PEN");

        Mockito.when(paymentService.registerPayment(eq(10), eq(5), any()))
                .thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(post("/payments/receipts/10/customer/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.path").value("/payments/receipts/10/customer/5"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    //Api Historial de Pago del Customer
    @Test
    void getPaymentsByCustomer_ShouldReturn200_WithData() throws Exception {
        Integer customerId = 5;

        PaymentResponseDTO dto = PaymentResponseDTO.builder()
                .amount(BigDecimal.valueOf(50))
                .paymentCurrency("PEN")
                .paymentStatus("PAID")
                .paymentDate(LocalDateTime.now())
                .customer(new CustomerSummaryDTO("Dolly", "Asto", "dolly@mail.com"))
                .service(new ServiceSummaryDTO("Internet Hogar", "Plan mensual"))
                .receipt(new ReceiptInfoDTO("0001", "Noviembre", LocalDate.now(),
                        BigDecimal.valueOf(150), BigDecimal.valueOf(0), "PEN", "PAID"))
                .build();

        Mockito.when(paymentService.getPaymentsByCustomer(customerId))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/payments/customers/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(50))
                .andExpect(jsonPath("$[0].paymentCurrency").value("PEN"))
                .andExpect(jsonPath("$[0].customer.names").value("Dolly"));
    }

    @Test
    void getPaymentsByCustomer_ShouldReturn200_WhenEmpty() throws Exception {
        Integer customerId = 5;

        Mockito.when(paymentService.getPaymentsByCustomer(customerId))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/payments/customers/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPaymentsByCustomer_ShouldReturn404_WhenCustomerNotFound() throws Exception {
        Integer customerId = 999;

        Mockito.when(paymentService.getPaymentsByCustomer(customerId))
                .thenThrow(new ResourceNotFoundException("Customer not found"));

        mockMvc.perform(get("/payments/customers/{customerId}", customerId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentsByCustomer_ShouldReturn500_WhenUnexpectedError() throws Exception {
        Integer customerId = 5;

        Mockito.when(paymentService.getPaymentsByCustomer(customerId))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/payments/customers/{customerId}", customerId))
                .andExpect(status().is5xxServerError());
    }




}