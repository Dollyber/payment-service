package com.payservice.paymentservice.util.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg) { super( msg); }
}