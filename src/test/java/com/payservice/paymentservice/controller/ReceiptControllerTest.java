package com.payservice.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payservice.paymentservice.dto.CustomerSummaryDTO;
import com.payservice.paymentservice.dto.ReceiptResponseDTO;
import com.payservice.paymentservice.dto.ServiceSummaryDTO;
import com.payservice.paymentservice.service.ReceiptService;
import com.payservice.paymentservice.util.exception.GlobalExceptionHandler;
import com.payservice.paymentservice.util.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReceiptControllerTest {

    private MockMvc mockMvc;
    private ReceiptService receiptService;

    @BeforeEach
    void setup() {
        //Crea una versión falsa del service
        receiptService = Mockito.mock(ReceiptService.class);

        //standaloneSetup: Crea un MockMvc solo con el Controller
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReceiptController(receiptService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getReceipts_shouldReturn200_whenDataExists() throws Exception {

        ReceiptResponseDTO dto = ReceiptResponseDTO.builder()
                .receiptNumber("00045231")
                .currency("PEN")
                .periodLabel("2025-11")
                .receiptAmount(new BigDecimal("150"))
                .pendingAmount(BigDecimal.ZERO)
                .receiptStatus("PAID")
                .dueDate(LocalDate.of(2025, 11, 30))
                .customer(new CustomerSummaryDTO("Amara", "Alfaro", "amara@mail.com"))
                .service(new ServiceSummaryDTO("Electricidad", "Plan mensual"))
                .build();

        when(receiptService.getReceiptsByServiceAndCustomer(100, 1))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/receipts/service/100/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].receiptNumber").value("00045231"))
                .andExpect(jsonPath("$[0].customer.names").value("Amara"))
                .andExpect(jsonPath("$[0].service.serviceName").value("Electricidad"));

    }

    @Test
    void getReceipts_customerNotFound_shouldReturn404() throws Exception {
        when(receiptService.getReceiptsByServiceAndCustomer(100, 1))
                .thenThrow(new ResourceNotFoundException("Customer not found"));

        mockMvc.perform(get("/receipts/service/100/customer/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Customer not found"))
                .andExpect(jsonPath("$.path").value("/receipts/service/100/customer/1"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getReceipts_serviceNotFound_shouldReturn404() throws Exception {

        when(receiptService.getReceiptsByServiceAndCustomer(100, 1))
                .thenThrow(new ResourceNotFoundException("Service not found"));

        mockMvc.perform(get("/receipts/service/100/customer/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Service not found"))
                .andExpect(jsonPath("$.path").value("/receipts/service/100/customer/1"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getReceipts_noReceipts_shouldReturn404() throws Exception {

        when(receiptService.getReceiptsByServiceAndCustomer(100, 1))
                .thenThrow(new ResourceNotFoundException("No receipts found for this customer/service"));

        mockMvc.perform(get("/receipts/service/100/customer/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("No receipts found for this customer/service"))
                .andExpect(jsonPath("$.path").value("/receipts/service/100/customer/1"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getReceipts_unexpectedError_shouldReturn500() throws Exception {

        when(receiptService.getReceiptsByServiceAndCustomer(100, 1))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/receipts/service/100/customer/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message")
                        .value("Internal server error"))
                .andExpect(jsonPath("$.path").value("/receipts/service/100/customer/1"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
