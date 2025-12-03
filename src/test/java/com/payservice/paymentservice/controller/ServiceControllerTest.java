package com.payservice.paymentservice.controller;

import com.payservice.paymentservice.dto.ServiceResponseDTO;
import com.payservice.paymentservice.service.ServiceService;
import com.payservice.paymentservice.util.exception.GlobalExceptionHandler;
import com.payservice.paymentservice.util.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ServiceControllerTest {
    private MockMvc mockMvc;
    private ServiceService serviceService;

    @BeforeEach
    void setup() {
        //Crea una versión falsa del service
        serviceService = Mockito.mock(ServiceService.class);

        //standaloneSetup: Crea un MockMvc solo con el Controller
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ServiceController(serviceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listServicesByCustomer_success() throws Exception {

        ServiceResponseDTO dto = new ServiceResponseDTO(
                "Internet", "Plan", "PEN",
                new BigDecimal("150"), new BigDecimal("20")
        );

        when(serviceService.getServicesByCustomer(1))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/services/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("Internet"))
                .andExpect(jsonPath("$[0].currency").value("PEN"));
    }

    @Test
    void listServicesByCustomer_customerNotFound() throws Exception {

        when(serviceService.getServicesByCustomer(1))
                .thenThrow(new ResourceNotFoundException("Customer not found"));

        mockMvc.perform(get("/services/customers/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Customer not found"))
                .andExpect(jsonPath("$.path").value("/services/customers/1"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
