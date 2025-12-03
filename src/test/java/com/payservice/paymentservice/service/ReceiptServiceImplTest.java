package com.payservice.paymentservice.service;

import com.payservice.paymentservice.dto.CustomerSummaryDTO;
import com.payservice.paymentservice.dto.ReceiptResponseDTO;
import com.payservice.paymentservice.dto.ServiceSummaryDTO;
import com.payservice.paymentservice.entity.Customer;
import com.payservice.paymentservice.entity.Receipt;
import com.payservice.paymentservice.entity.ServiceEntity;
import com.payservice.paymentservice.mapper.ReceiptMapper;
import com.payservice.paymentservice.repository.CustomerRepository;
import com.payservice.paymentservice.repository.ReceiptRepository;
import com.payservice.paymentservice.repository.ServiceRepository;
import com.payservice.paymentservice.service.impl.ReceiptServiceImpl;
import com.payservice.paymentservice.util.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class ReceiptServiceImplTest {

    //Mocks (Mockito), que son objetos falsos que simulan las dependencias.
    @Mock private ReceiptRepository receiptRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private ReceiptMapper receiptMapper;

    @InjectMocks
    private ReceiptServiceImpl receiptService;

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
        receipt.setReceiptAmount(new BigDecimal("150.00"));
        receipt.setPendingAmount(new BigDecimal("0.00"));
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
    void getReceipts_CustomerNotFound_ShouldThrowNotFound() {

        when(customerRepository.findById(1))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> receiptService.getReceiptsByServiceAndCustomer(100, 1)
        );

        assertEquals("Customer not found", ex.getMessage());
    }

    @Test
    void getReceipts_ServiceNotFound_ShouldThrowNotFound() {

        when(customerRepository.findById(1))
                .thenReturn(Optional.of(customer));

        when(serviceRepository.findById(100))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> receiptService.getReceiptsByServiceAndCustomer(100, 1)
        );

        assertEquals("Service not found", ex.getMessage());
    }

    @Test
    void getReceipts_NoReceipts_ShouldThrowNotFound() {

        when(customerRepository.findById(1))
                .thenReturn(Optional.of(customer));

        when(serviceRepository.findById(100))
                .thenReturn(Optional.of(serviceEntity));

        when(receiptRepository.findByServiceIdAndCustomerIdOrderByDueDateDesc(100, 1))
                .thenReturn(List.of()); // lista vacía

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> receiptService.getReceiptsByServiceAndCustomer(100, 1)
        );

        assertEquals("No receipts found for this customer/service", ex.getMessage());
    }

    @Test
    void getReceipts_ShouldReturnReceiptList_WhenDataExists() {
        ReceiptResponseDTO mappedDto = ReceiptResponseDTO.builder()
                .receiptNumber("00000010")
                .periodLabel("2025-11")
                .currency("PEN")
                .receiptStatus("PAID")
                .receiptAmount(new BigDecimal("150"))
                .pendingAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.of(2025, 11, 30))
                .customer(new CustomerSummaryDTO(customer.getNames(), customer.getLastname(), customer.getEmail()))
                .service(new ServiceSummaryDTO(serviceEntity.getServiceName(), serviceEntity.getDescription()))
                .build();

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        when(serviceRepository.findById(100)).thenReturn(Optional.of(serviceEntity));

        when(receiptRepository.findByServiceIdAndCustomerIdOrderByDueDateDesc(100, 1))
                .thenReturn(List.of(receipt));

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        when(serviceRepository.findById(100)).thenReturn(Optional.of(serviceEntity));

        when(receiptMapper.toFullReceiptInfo(receipt, customer, serviceEntity))
                .thenReturn(mappedDto);

        List<ReceiptResponseDTO> result =
                receiptService.getReceiptsByServiceAndCustomer(100, 1);

        assertEquals(1, result.size());
        assertEquals("00000010", result.get(0).getReceiptNumber());
        assertEquals("PEN", result.get(0).getCurrency());
    }


}
