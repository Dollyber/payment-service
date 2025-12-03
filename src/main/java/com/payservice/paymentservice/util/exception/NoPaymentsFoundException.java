package com.payservice.paymentservice.util.exception;

public class NoPaymentsFoundException extends RuntimeException {
    public NoPaymentsFoundException(String message) {
        super(message);
    }
}
