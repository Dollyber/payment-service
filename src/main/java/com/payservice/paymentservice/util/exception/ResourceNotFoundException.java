package com.payservice.paymentservice.util.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg) { super( msg); }
}