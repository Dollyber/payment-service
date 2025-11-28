package com.payservice.paymentservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ResourceNotFoundException extends ResponseStatusException {
    public ResourceNotFoundException(String msg) { super(HttpStatus.NOT_FOUND, msg); }
}