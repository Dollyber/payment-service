package com.payservice.paymentservice.util.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.payservice.paymentservice.dto.ErrorResponseDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ErrorResponseDTO buildError(HttpStatus status, String message, WebRequest request) {
        return new ErrorResponseDTO(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getDescription(false).replace("uri=", ""),
                java.time.LocalDateTime.now()
        );
    }

    // RN3 — Pago excede saldo
    @ExceptionHandler(OverpaymentException.class)
    public ResponseEntity<ErrorResponseDTO> handleOverpayment(OverpaymentException ex, WebRequest request) {
        ErrorResponseDTO error = buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
        return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // RN6 — Recibo anterior sin pagar
    @ExceptionHandler(PendingReceiptException.class)
    public ResponseEntity<ErrorResponseDTO> handlePendingReceipt(PendingReceiptException ex, WebRequest request) {
        ErrorResponseDTO error = buildError(HttpStatus.CONFLICT, ex.getMessage(), request);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // RN1 — Moneda inválida (validación simple)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        ErrorResponseDTO error = buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Error cuando no encuentra un recurso
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponseDTO error = buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
        //return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    //public ResourceNotFoundException(String msg) { super(HttpStatus.NOT_FOUND, msg); }

    // Type mismatch → Ej: enviar string donde va int
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        ErrorResponseDTO error = buildError(HttpStatus.BAD_REQUEST, "Invalid parameter format", request);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Fallback — cualquier excepción no controlada
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneral(Exception ex, WebRequest request) {
        ErrorResponseDTO error = buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    //No hay pagos para el customer
    @ExceptionHandler(NoPaymentsFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoPayments(NoPaymentsFoundException ex, WebRequest request) {
        ErrorResponseDTO error = buildError(HttpStatus.NO_CONTENT, ex.getMessage(), request);
        return new ResponseEntity<>(error, HttpStatus.NO_CONTENT);
    }
}
