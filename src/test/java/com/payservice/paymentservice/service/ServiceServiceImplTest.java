package com.payservice.paymentservice.service;

import com.payservice.paymentservice.dto.ServiceResponseDTO;
import com.payservice.paymentservice.entity.Customer;
import com.payservice.paymentservice.entity.Receipt;
import com.payservice.paymentservice.entity.ServiceEntity;
import com.payservice.paymentservice.mapper.ReceiptMapper;
import com.payservice.paymentservice.mapper.ServiceMapper;
import com.payservice.paymentservice.repository.CustomerRepository;
import com.payservice.paymentservice.repository.PaymentRepository;
import com.payservice.paymentservice.repository.ReceiptRepository;
import com.payservice.paymentservice.repository.ServiceRepository;
import com.payservice.paymentservice.service.impl.ReceiptServiceImpl;
import com.payservice.paymentservice.service.impl.ServiceServiceImpl;
import com.payservice.paymentservice.util.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class ServiceServiceImplTest {

    //Mocks (Mockito), que son objetos falsos que simulan las dependencias.
    @Mock
    private ReceiptRepository receiptRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private ServiceMapper serviceMapper;

    @InjectMocks
    private ServiceServiceImpl serviceService;

    private Receipt receipt;
    private Customer customer;
    private ServiceEntity serviceEntity;

    @BeforeEach
        //Se ejecuta antes de cada prueba
    void setUp() {
        receipt = new Receipt();
        receipt.setReceiptId(10);
        receipt.setReceiptNumber("00000010");
        receipt.setServiceId(100);
        receipt.setCustomerId(1);
        receipt.setCurrency("PEN");
        receipt.setReceiptAmount(new BigDecimal("100.00"));
        receipt.setPendingAmount(new BigDecimal("20.00"));
        receipt.setReceiptStatus("PAID");
        receipt.setPeriodLabel("2025-11");
        receipt.setDueDate(LocalDate.of(2025, 11, 30));
        receipt.setDateRegist(LocalDateTime.now());

        customer = new Customer();
        customer.setCustomerId(1);
        customer.setNames("Dolly");
        customer.setLastname("Asto");
        customer.setEmail("dolly@mail.com");

        serviceEntity = new ServiceEntity();
        serviceEntity.setServiceId(100);
        serviceEntity.setCustomerId(1);
        serviceEntity.setServiceName("Internet Hogar");
        serviceEntity.setDescription("Plan mensual");
        serviceEntity.setIsActive(true);
    }

    @Test
    void getServicesByCustomer_customerNotFound() {

        when(customerRepository.findById(1))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> serviceService.getServicesByCustomer(1)
        );

        assertTrue(ex.getMessage().contains("Customer not found"));
    }

    @Test
    void getServicesByCustomer_noServices() {

        when(customerRepository.findById(1))
                .thenReturn(Optional.of(customer));

        when(serviceRepository.findByCustomerId(1))
                .thenReturn(Collections.emptyList());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> serviceService.getServicesByCustomer(1)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("customer has no registered services"));
    }

    @Test
    void getServicesByCustomer_serviceHasNoReceipts() {

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        when(serviceRepository.findByCustomerId(1))
                .thenReturn(List.of(serviceEntity));

        when(receiptRepository.findByServiceIdAndCustomerId(100, 1))
                .thenReturn(Collections.emptyList());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> serviceService.getServicesByCustomer(1)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("has no receipts for this customer"));
    }

    @Test
    void getServicesByCustomer_singleCurrency() {

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(serviceRepository.findByCustomerId(1)).thenReturn(List.of(serviceEntity));

        //Otro objecto receipt para pruebas
        Receipt r2 = new Receipt();
        r2.setCurrency("PEN");
        r2.setReceiptAmount(new BigDecimal("50"));
        r2.setPendingAmount(BigDecimal.ZERO);

        when(receiptRepository.findByServiceIdAndCustomerId(100, 1))
                .thenReturn(List.of(receipt, r2));

        when(serviceMapper.toServiceResponseDTO(any(), any(), any(), any()))
                .thenReturn(new ServiceResponseDTO("Internet", "Plan", "PEN",
                        new BigDecimal("150"), new BigDecimal("20")));

        List<ServiceResponseDTO> result = serviceService.getServicesByCustomer(1);

        assertEquals(1, result.size());
        assertEquals("PEN", result.get(0).getCurrency());
        assertEquals(new BigDecimal("150"), result.get(0).getAmount());
    }

    @Test
    void getServicesByCustomer_multiCurrency() {

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(serviceRepository.findByCustomerId(1)).thenReturn(List.of(serviceEntity));

        Receipt r2 = new Receipt();
        r2.setCurrency("USD");
        r2.setReceiptAmount(new BigDecimal("40"));
        r2.setPendingAmount(BigDecimal.ZERO);

        when(receiptRepository.findByServiceIdAndCustomerId(100, 1))
                .thenReturn(List.of(receipt, r2));

        when(serviceMapper.toServiceResponseDTO(any(), any(), any(), any()))
                .thenReturn(new ServiceResponseDTO("Internet", "Plan",
                        "MULTIMONEDA", new BigDecimal("140"), BigDecimal.ZERO));

        List<ServiceResponseDTO> result = serviceService.getServicesByCustomer(1);

        assertEquals("MULTIMONEDA", result.get(0).getCurrency());
    }
}
