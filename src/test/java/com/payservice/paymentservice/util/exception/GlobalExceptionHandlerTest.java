package com.payservice.paymentservice.util.exception;

import com.payservice.paymentservice.dto.ErrorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import org.mockito.Mockito;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest request;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
        request = Mockito.mock(WebRequest.class);
        Mockito.when(request.getDescription(false))
                .thenReturn("uri=/test-endpoint");
    }

    @Test
    void handleOverpayment_ShouldReturn422() {
        OverpaymentException ex = new OverpaymentException("Exceeds allowed amount");

        ResponseEntity<ErrorResponseDTO> response = handler.handleOverpayment(ex, request);

        assertEquals(422, response.getStatusCode().value());
        assertEquals("Exceeds allowed amount", response.getBody().getMessage());
        assertEquals("/test-endpoint", response.getBody().getPath());
    }

    @Test
    void handlePendingReceipt_ShouldReturn409() {
        PendingReceiptException ex = new PendingReceiptException("Pending debt found");

        ResponseEntity<ErrorResponseDTO> response = handler.handlePendingReceipt(ex, request);

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Pending debt found", response.getBody().getMessage());
    }

    @Test
    void handleIllegalArgument_ShouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid currency");

        ResponseEntity<ErrorResponseDTO> response = handler.handleIllegalArgument(ex, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid currency", response.getBody().getMessage());
    }

    @Test
    void handleNotFound_ShouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Customer not found");

        ResponseEntity<ErrorResponseDTO> response = handler.handleNotFound(ex, request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Customer not found", response.getBody().getMessage());
    }

    @Test
    void handleTypeMismatch_ShouldReturn400() {
        MethodArgumentTypeMismatchException ex =
                Mockito.mock(MethodArgumentTypeMismatchException.class);

        ResponseEntity<ErrorResponseDTO> response = handler.handleTypeMismatch(ex, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid parameter format", response.getBody().getMessage());
    }

    @Test
    void handleGeneral_ShouldReturn500() {
        Exception ex = new Exception("Unexpected error");

        ResponseEntity<ErrorResponseDTO> response = handler.handleGeneral(ex, request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Internal server error", response.getBody().getMessage());
    }
}