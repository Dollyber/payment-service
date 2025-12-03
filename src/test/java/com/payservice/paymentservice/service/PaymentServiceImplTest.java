package com.payservice.paymentservice.service;

import com.payservice.paymentservice.dto.PaymentRequestDTO;
import com.payservice.paymentservice.dto.PaymentResponseDTO;
import com.payservice.paymentservice.entity.*;
import com.payservice.paymentservice.service.impl.PaymentServiceImpl;
import com.payservice.paymentservice.util.ExchangeRateConstants;
import com.payservice.paymentservice.util.exception.NoPaymentsFoundException;
import com.payservice.paymentservice.util.exception.OverpaymentException;
import com.payservice.paymentservice.util.exception.PendingReceiptException;
import com.payservice.paymentservice.util.exception.ResourceNotFoundException;
import com.payservice.paymentservice.mapper.PaymentMapper;
import com.payservice.paymentservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class PaymentServiceImplTest {

    //Mocks (Mockito), que son objetos falsos que simulan las dependencias.
    @Mock private ReceiptRepository receiptRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMapper paymentMapper;

    @InjectMocks //Crea una instancia real de PaymentServiceImpl y le inyecta los mocks.
    private PaymentServiceImpl paymentService;

    //Datos de muestra reutilizados en pruebas
    private Receipt receipt;
    private Customer customer;
    private ServiceEntity serviceEntity;

    @BeforeEach //Se ejecuta antes de cada prueba
    void setUp() {
        receipt = new Receipt();
        receipt.setReceiptId(10);
        receipt.setReceiptNumber("00000010");
        receipt.setServiceId(100);
        receipt.setCustomerId(1);
        receipt.setCurrency("PEN");
        receipt.setReceiptAmount(new BigDecimal("150.00"));
        receipt.setPendingAmount(new BigDecimal("50.00"));
        receipt.setReceiptStatus("PARTIALLY_PAID");
        receipt.setDueDate(LocalDate.now().plusDays(1));
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

    //RN2 — Un servicio puede pagarse parcial o totalmente
    //RN3 — Los pagos parciales no pueden exceder el saldo pendiente
    //RN5 — El servicio se considera “pagado” cuando el saldo pendiente llega a cero
    @Test
    void registerPayment_success_partialPayment() {
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("10.00"));
        req.setPaymentCurrency("PEN");

        //Simular el comportamiento del repositorio.
        // Recibo existe
        when(receiptRepository.findById(receipt.getReceiptId())).thenReturn(Optional.of(receipt));
        //verificar que el recibo pertenede al cliente

        // No hay recibos pendientes sin pagar
        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(
                anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(serviceRepository.findById(100)).thenReturn(Optional.of(serviceEntity));

        //Simula que JPA guarda el recibo
        //Devuelve tal cual el recibo modificado por el service
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Pago simulado guardado  (repository)
        Payment savedPayment = new Payment();
        savedPayment.setPaymentId(999);
        savedPayment.setReceiptId(receipt.getReceiptId());
        savedPayment.setCustomerId(customer.getCustomerId());
        savedPayment.setAmount(req.getAmount());
        savedPayment.setPaymentCurrency(req.getPaymentCurrency());
        savedPayment.setExchangeRate(ExchangeRateConstants.DEFAULT_RATE);
        savedPayment.setPreviousPendingAmount(new BigDecimal("50.00"));
        savedPayment.setNewPendingAmount(new BigDecimal("40.00"));
        savedPayment.setPaymentStatus("PARTIALLY_PAID");
        savedPayment.setPaymentDate(LocalDateTime.now());

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // Mapper output
        PaymentResponseDTO dto = PaymentResponseDTO.builder()
                .amount(savedPayment.getAmount())
                .paymentCurrency(savedPayment.getPaymentCurrency())
                .exchangeRate(savedPayment.getExchangeRate())
                .paymentStatus(savedPayment.getPaymentStatus())
                .paymentDate(savedPayment.getPaymentDate())
                .build();
        when(paymentMapper.toPaymentResponse(any(Payment.class), any(Customer.class), any(ServiceEntity.class), any(Receipt.class)))
                .thenReturn(dto);

        // Act
        PaymentResponseDTO result = paymentService.registerPayment(receipt.getReceiptId(), customer.getCustomerId(), req);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("10.00"), result.getAmount());
        assertEquals("PEN", result.getPaymentCurrency());
        assertEquals("PARTIALLY_PAID", result.getPaymentStatus());
        verify(receiptRepository).save(any(Receipt.class));
        verify(paymentRepository).save(any(Payment.class));
        verify(paymentMapper).toPaymentResponse(any(Payment.class), any(Customer.class), any(ServiceEntity.class), any(Receipt.class));
    }

    @Test
    void registerPayment_nonPositiveAmount_throwsIllegalArgumentException() {
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(BigDecimal.ZERO);
        req.setPaymentCurrency("PEN");

        when(receiptRepository.findById(receipt.getReceiptId()))
                .thenReturn(Optional.of(receipt));

        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(
                anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        Integer receiptId = receipt.getReceiptId();
        Integer customerId = receipt.getCustomerId();

        //Aquí ejecutamos el método real del service pero esperamos que lance una excepción.
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.registerPayment(receiptId, customerId, req)
        );

        //confirma que el mensaje de error realmente habla del amount inválido.
        assertTrue(ex.getMessage().toLowerCase().contains("amount must be positive"));

        // Verificamos que no se intentó guardar nada
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void registerPayment_nullAmount_throwsIllegalArgumentException() {
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(null);
        req.setPaymentCurrency("PEN");

        when(receiptRepository.findById(receipt.getReceiptId()))
                .thenReturn(Optional.of(receipt));

        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(
                anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        Integer receiptId = receipt.getReceiptId();
        Integer customerId = receipt.getCustomerId();

        //Aquí ejecutamos el método real del service pero esperamos que lance una excepción.
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.registerPayment(receiptId, customerId, req)
        );


        //confirma que el mensaje de error realmente habla del amount inválido.
        assertTrue(ex.getMessage().toLowerCase().contains("amount must be positive"));

        // Verificamos que no se intentó guardar nada
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    //RN1 — Solo se permite pagar en PEN o USD
    @Test
    void registerPayment_invalidCurrency_throwsIllegalArgumentException() {
        // Arrange
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("10.00"));
        req.setPaymentCurrency("EUR"); // invalid currency

        Integer receiptId = receipt.getReceiptId();
        Integer customerId = receipt.getCustomerId();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.registerPayment(receiptId, customerId, req)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("only pen or usd"));

        // Verificamos que no se intentó guardar nada
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void registerPayment_nullCurrency_throwsIllegalArgumentException() {
        // Arrange
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("10.00"));
        req.setPaymentCurrency(null);

        Integer receiptId = receipt.getReceiptId();
        Integer customerId = receipt.getCustomerId();

        // Act & Assert: validar que se lanza IllegalArgumentException por RN1
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.registerPayment(
                        receiptId,
                        customerId,
                        req
                )
        );

        // Verificamos el mensaje (parte importante)
        assertTrue(ex.getMessage().toLowerCase().contains("currency cannot be null"));

        // Verificamos que no se intentó guardar nada
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void registerPayment_receiptCurrencyNull_throwsIllegalArgumentException() {

        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("10"));
        req.setPaymentCurrency("PEN");

        // Recibo con currency NULL
        receipt.setCurrency(null);

        when(receiptRepository.findById(receipt.getReceiptId()))
                .thenReturn(Optional.of(receipt));

        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(
                anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        Integer receiptId = receipt.getReceiptId();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.registerPayment(receiptId, 1, req)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("currency cannot be null"));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    //

    @Test
    void determineExchangeRate_bothNull_throwsIllegalArgumentException_reflection() throws Exception {
        Method method = PaymentServiceImpl.class.getDeclaredMethod("determineExchangeRate", String.class, String.class);
        method.setAccessible(true);
        try {
            method.invoke(paymentService, null, null);
            fail("Expected IllegalArgumentException");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertTrue(e.getCause().getMessage().toLowerCase().contains("currency cannot be null"));
        }
    }


    @Test
    void registerPayment_lowercaseCurrency_acceptedNormally() {
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("10"));
        req.setPaymentCurrency("pen");

        when(receiptRepository.findById(receipt.getReceiptId())).thenReturn(Optional.of(receipt));

        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(anyInt(),anyInt(),any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        when(serviceRepository.findById(100)).thenReturn(Optional.of(serviceEntity));

        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(receiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(paymentMapper.toPaymentResponse(any(), any(), any(), any()))
                .thenReturn(new PaymentResponseDTO());

        PaymentResponseDTO result = paymentService.registerPayment(receipt.getReceiptId(), 1, req);

        assertNotNull(result);
    }

    //RN3: Los pagos parciales no pueden exceder el saldo pendiente
    @Test
    void registerPayment_overpayment_throwsOverpaymentException() {
        // Arrange: attempt to pay more than pending
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("100.00")); // pending 50
        req.setPaymentCurrency("PEN");

        when(receiptRepository.findById(receipt.getReceiptId())).thenReturn(Optional.of(receipt));
        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(
                anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        Integer receiptId = receipt.getReceiptId();
        Integer customerId = receipt.getCustomerId();

        // Act & Assert
        OverpaymentException ex = assertThrows(
                OverpaymentException.class,
                () -> paymentService.registerPayment(
                        receiptId,
                        customerId,
                        req
                )
        );

        assertTrue(ex.getMessage().contains("exceeds pending amount"));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void registerPayment_overpaymentWithCurrencyConversion_throwsOverpaymentException() {
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("30")); // 30 * 3.5 = 105 > pending 50
        req.setPaymentCurrency("USD");

        when(receiptRepository.findById(receipt.getReceiptId()))
                .thenReturn(Optional.of(receipt));
        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(
                anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        Integer receiptId = receipt.getReceiptId();
        Integer customerId = receipt.getCustomerId();

        // Act & Assert
        OverpaymentException ex = assertThrows(OverpaymentException.class, () ->
                paymentService.registerPayment(receiptId, customerId, req)
        );

        assertTrue(ex.getMessage().contains("exceeds pending amount"));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void registerPayment_previousUnpaid_throwsPendingReceiptException() {
        // Arrange:
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("10.00"));
        req.setPaymentCurrency("PEN");

        // recibo previo
        Receipt prev = new Receipt();
        prev.setReceiptId(9);
        prev.setServiceId(receipt.getServiceId());
        prev.setCustomerId(receipt.getCustomerId());
        prev.setReceiptStatus("PARTIALLY_PAID");
        prev.setDueDate(receipt.getDueDate().minusDays(10));

        when(receiptRepository.findById(receipt.getReceiptId())).thenReturn(Optional.of(receipt));

        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(
                eq(receipt.getServiceId()), eq(receipt.getCustomerId()), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(prev));

        Integer receiptId = receipt.getReceiptId();
        Integer customerId = receipt.getCustomerId();

        PendingReceiptException ex = assertThrows(
                PendingReceiptException.class,
                () -> paymentService.registerPayment(
                        receiptId,
                        customerId,
                        req
                )
        );

        // Validamos el mensaje de error
        assertTrue(
                ex.getMessage().toLowerCase().contains("previous receipts are unpaid")
        );

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void registerPayment_previousReceiptsAllPaid_allowsPayment() {
        // Arrange
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("10.00"));
        req.setPaymentCurrency("PEN");

        // receipt actual (ya en @BeforeEach)
        when(receiptRepository.findById(receipt.getReceiptId())).thenReturn(Optional.of(receipt));

        // previous receipts pero todos PAID
        Receipt prevPaid = new Receipt();
        prevPaid.setReceiptId(9);
        prevPaid.setServiceId(receipt.getServiceId());
        prevPaid.setCustomerId(receipt.getCustomerId());
        prevPaid.setReceiptStatus("PAID");
        prevPaid.setDueDate(receipt.getDueDate().minusDays(10));

        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(
                eq(receipt.getServiceId()), eq(receipt.getCustomerId()), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(prevPaid));

        // mocks para continuar el flujo
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(serviceRepository.findById(receipt.getServiceId())).thenReturn(Optional.of(serviceEntity));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentMapper.toPaymentResponse(any(), any(), any(), any())).thenReturn(new PaymentResponseDTO());

        // Act
        PaymentResponseDTO resp = paymentService.registerPayment(receipt.getReceiptId(), 1, req);

        // Assert - no exception, pago guardado (mapper fue invocado)
        assertNotNull(resp);
        verify(paymentRepository).save(any(Payment.class));
    }


    @Test
    void registerPayment_receiptNotFound_throwsResourceNotFoundException() {
        // Arrange
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("10.00"));
        req.setPaymentCurrency("PEN");

        when(receiptRepository.findById(anyInt())).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.registerPayment(9999, 1, req)
        );

        // Validamos el mensaje de error
        assertTrue(
                ex.getMessage().toLowerCase().contains("receipt not found")
        );

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void registerPayment_receiptDoesNotBelongToCustomer_throwsResourceNotFound() {
        // Arrange
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("10"));
        req.setPaymentCurrency("PEN");

        when(receiptRepository.findById(receipt.getReceiptId()))
                .thenReturn(Optional.of(receipt));

        Integer receiptId = receipt.getReceiptId();

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.registerPayment(
                        receiptId,
                        2,
                        req
                )
        );

        // Validamos el mensaje de error
        assertTrue(
                ex.getMessage().toLowerCase().contains("receipt does not belong to customer")
        );

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    //RN5: El servicio se considera “pagado” cuando el saldo pendiente llega a cero
    @Test
    void registerPayment_receiptAlreadyPaid_throwsIllegalArgumentException() {
        // Arrange:
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("10.00"));
        req.setPaymentCurrency("PEN");

        // set receipt status to PAID
        receipt.setReceiptStatus("PAID");

        when(receiptRepository.findById(receipt.getReceiptId())).thenReturn(Optional.of(receipt));

        Integer receiptId = receipt.getReceiptId();
        Integer customerId = receipt.getCustomerId();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.registerPayment(
                        receiptId,
                        customerId,
                        req
                )
        );

        assertTrue(ex.getMessage().toLowerCase().contains("already paid"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void determineExchangeRate_sameCurrencyUSD_returnsDefaultRate() {
        // Arrange:
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("10"));
        req.setPaymentCurrency("USD");

        receipt.setCurrency("USD");

        // Mock: el recibo existe
        when(receiptRepository.findById(receipt.getReceiptId()))
                .thenReturn(Optional.of(receipt));

        // Mock: no hay recibos anteriores impagos
        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(
                anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        //processPayment
        // Mock: JPA save devuelve el mismo objeto
        when(receiptRepository.save(any(Receipt.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Capturamos el Payment que será guardado
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Mock: customer & service existen
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(serviceRepository.findById(receipt.getServiceId())).thenReturn(Optional.of(serviceEntity));

        // Mock mapper (no importa devolver fields reales para este test)
        when(paymentMapper.toPaymentResponse(any(), any(), any(), any()))
                .thenReturn(new PaymentResponseDTO());

        // Act
        paymentService.registerPayment(receipt.getReceiptId(), 1, req);

        // Assert — validar que el tipo de cambio fue DEFAULT_RATE
        //valida que se llamo a paymentRepository.save(...)
        //Captura el objeto EXACTO que se envió como parámetro antes deque el mock "responda"
        verify(paymentRepository).save(paymentCaptor.capture());

        //Guarda el EXACTO Payment que se creó dentro del service,
        Payment capturedPayment = paymentCaptor.getValue();

        assertEquals(
                ExchangeRateConstants.DEFAULT_RATE,
                capturedPayment.getExchangeRate(),
                "El tipo de cambio debe ser DEFAULT_RATE cuando ambas monedas son USD"
        );

        // previousPending
        assertEquals(new BigDecimal("50.00"), capturedPayment.getPreviousPendingAmount());

        // newPending
        BigDecimal expectedNewPending = new BigDecimal("40.00");
        assertEquals(expectedNewPending, capturedPayment.getNewPendingAmount());

        // Estado
        assertEquals("PARTIALLY_PAID", capturedPayment.getPaymentStatus());
    }

    @Test
    void convertAmount_penToUsd_updatesPendingCorrectly() {
        // Arrange -----------------------------------------
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("3.50"));
        req.setPaymentCurrency("PEN");

        // Recibo está en USD → pago llega en PEN
        receipt.setCurrency("USD");             // moneda del recibo

        // Mock repositorios
        when(receiptRepository.findById(receipt.getReceiptId()))
                .thenReturn(Optional.of(receipt));

        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(
                anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        when(receiptRepository.save(any(Receipt.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Capturador del pago
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Mock customer, service y mapper
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(serviceRepository.findById(receipt.getServiceId())).thenReturn(Optional.of(serviceEntity));
        when(paymentMapper.toPaymentResponse(any(), any(), any(), any()))
                .thenReturn(new PaymentResponseDTO());

        // Act ---------------------------------------------
        paymentService.registerPayment(receipt.getReceiptId(), 1, req);

        // Assert -------------------------------------------
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();

        // Se usó la tasa correcta
        assertEquals(ExchangeRateConstants.USD_RATE, saved.getExchangeRate());

        // previousPending debe ser el del receipt original
        assertEquals(new BigDecimal("50.00"), saved.getPreviousPendingAmount());

        // VERIFICAR MONTO CONVERTIDO (1 USD)
        BigDecimal expectedConvertedAmount = new BigDecimal("1.00");
        BigDecimal actualConvertedUsed = saved.getPreviousPendingAmount()
                .subtract(saved.getNewPendingAmount());

        assertEquals(expectedConvertedAmount, actualConvertedUsed);

        // newPending debe restar el monto CONVERTIDO → 3.80 / 3.80 = 1 USD
        BigDecimal expectedNewPending = new BigDecimal("49.00");
        assertEquals(expectedNewPending, saved.getNewPendingAmount());

        // Estado
        assertEquals("PARTIALLY_PAID", saved.getPaymentStatus());
    }

    @Test
    void convertAmount_usdToPen_updatesCorrectly() {
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("10"));
        req.setPaymentCurrency("USD");

        when(receiptRepository.findById(receipt.getReceiptId())).thenReturn(Optional.of(receipt));

        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(anyInt(),anyInt(),any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        when(serviceRepository.findById(receipt.getServiceId())).thenReturn(Optional.of(serviceEntity));

        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(receiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(paymentMapper.toPaymentResponse(any(), any(), any(), any()))
                .thenReturn(new PaymentResponseDTO());

        ArgumentCaptor<Payment> cap = ArgumentCaptor.forClass(Payment.class);

        paymentService.registerPayment(receipt.getReceiptId(), 1, req);

        verify(paymentRepository).save(cap.capture());
        Payment saved = cap.getValue();

        BigDecimal expected = new BigDecimal("10").multiply(ExchangeRateConstants.USD_RATE); //35
        BigDecimal actual = saved.getPreviousPendingAmount().subtract(saved.getNewPendingAmount()); //50-15

        assertEquals(expected, actual);
    }

    @Test
    void registerPayment_fullPayment_setsStatusPaid() {
        // Arrange
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("50.00")); // igual al pendingAmount
        req.setPaymentCurrency("PEN");

        // Mock: recibo existe
        when(receiptRepository.findById(receipt.getReceiptId()))
                .thenReturn(Optional.of(receipt));

        // Mock: no hay recibos anteriores pendientes
        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(
                anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Mock: save debe devolver el mismo receipt modificado
        when(receiptRepository.save(any(Receipt.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Capturar payment
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(i -> i.getArgument(0));

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(serviceRepository.findById(100)).thenReturn(Optional.of(serviceEntity));

        // Mapper fake
        when(paymentMapper.toPaymentResponse(any(), any(), any(), any()))
                .thenReturn(new PaymentResponseDTO());

        // Act
        paymentService.registerPayment(receipt.getReceiptId(), 1, req);

        // Assert — 1 Verificar que receipt quedó pagado
        assertEquals(new BigDecimal("0.00"), receipt.getPendingAmount());
        assertEquals("PAID", receipt.getReceiptStatus());

        // Assert — 2 Verificar Payment creado correctamente
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment capturedPayment = paymentCaptor.getValue();

        assertEquals("PAID", capturedPayment.getPaymentStatus());
        assertEquals(new BigDecimal("50.00"), capturedPayment.getAmount());
        assertEquals(new BigDecimal("0.00"), capturedPayment.getNewPendingAmount());

        // Assert — 3 Verificar que receipt fue guardado
        verify(receiptRepository).save(any(Receipt.class));
    }

    @Test
    void registerPayment_fullPaymentInUsd_setsPaid() {
        // pending = 50
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("10")); // 14.2857 * 3.5 = 50
        req.setPaymentCurrency("USD");

        //cambiar a pendiente a 35
        receipt.setPendingAmount(new BigDecimal("35"));


        when(receiptRepository.findById(receipt.getReceiptId())).thenReturn(Optional.of(receipt));

        when(receiptRepository.findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        when(serviceRepository.findById(receipt.getServiceId())).thenReturn(Optional.of(serviceEntity));

        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(receiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(paymentMapper.toPaymentResponse(any(), any(), any(), any()))
                .thenReturn(new PaymentResponseDTO());

        paymentService.registerPayment(receipt.getReceiptId(), 1, req);

        assertEquals("PAID", receipt.getReceiptStatus());
    }

    //Get: Historial de pagos del customer
    @Test
    void getPaymentsByCustomer_customerNotFound_throwsException() {

        when(customerRepository.findById(2))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                paymentService.getPaymentsByCustomer(2));

        //confirma que el mensaje de error.
        assertTrue(ex.getMessage().toLowerCase().contains("customer not found"));

        // Verificamos que no se intentó guardar nada
        verify(paymentRepository, never()).findByCustomerIdOrderByPaymentDateDesc(anyInt());
    }

    @Test
    void getPaymentsByCustomer_noPayments_throwsNoPaymentsFoundException() {

        when(customerRepository.findById(1))
                .thenReturn(Optional.of(customer));

        when(paymentRepository.findByCustomerIdOrderByPaymentDateDesc(1))
                .thenReturn(Collections.emptyList());

        NoPaymentsFoundException ex = assertThrows(NoPaymentsFoundException.class, () ->
                paymentService.getPaymentsByCustomer(1));

        //confirma que el mensaje de error.
        assertTrue(ex.getMessage().toLowerCase().contains("customer has no registered payments"));

        verify(paymentRepository).findByCustomerIdOrderByPaymentDateDesc(1);
        verify(paymentMapper, never()).toPaymentResponse(any(), any(), any(), any());
    }

    @Test
    void getPaymentsByCustomer_withPayments_receiptExists_mapsCorrectly() {

        when(customerRepository.findById(1))
                .thenReturn(Optional.of(customer));

        Payment payment = new Payment();
        payment.setPaymentId(10);
        payment.setReceiptId(10);
        payment.setCustomerId(1);

        when(paymentRepository.findByCustomerIdOrderByPaymentDateDesc(1))
                .thenReturn(List.of(payment));

        when(receiptRepository.findById(10))
                .thenReturn(Optional.of(receipt));

        when(serviceRepository.findById(100))
                .thenReturn(Optional.of(serviceEntity));

        // mapper response
        PaymentResponseDTO dto = PaymentResponseDTO.builder().amount(BigDecimal.TEN).build();
        when(paymentMapper.toPaymentResponse(payment, customer, serviceEntity, receipt))
                .thenReturn(dto);

        List<PaymentResponseDTO> result = paymentService.getPaymentsByCustomer(1);

        assertEquals(1, result.size());
        assertEquals(BigDecimal.TEN, result.get(0).getAmount());

        verify(paymentMapper).toPaymentResponse(payment, customer, serviceEntity, receipt);
    }

    @Test
    void getPaymentsByCustomer_withPayments_receiptMissing_serviceNull() {

        when(customerRepository.findById(1))
                .thenReturn(Optional.of(customer));

        Payment payment = new Payment();
        payment.setPaymentId(20);
        payment.setReceiptId(999);
        payment.setCustomerId(1);

        when(paymentRepository.findByCustomerIdOrderByPaymentDateDesc(1))
                .thenReturn(List.of(payment));

        // receipt not found
        when(receiptRepository.findById(999))
                .thenReturn(Optional.empty());

        // mapper must receive payment + customer + null + null
        PaymentResponseDTO dto = PaymentResponseDTO.builder().paymentCurrency("PEN").build();

        when(paymentMapper.toPaymentResponse(eq(payment), eq(customer), isNull(), isNull()))
                .thenReturn(dto);

        List<PaymentResponseDTO> result = paymentService.getPaymentsByCustomer(1);

        assertEquals(1, result.size());
        assertEquals("PEN", result.get(0).getPaymentCurrency());

        verify(paymentMapper).toPaymentResponse(eq(payment), eq(customer), isNull(), isNull());
    }

}