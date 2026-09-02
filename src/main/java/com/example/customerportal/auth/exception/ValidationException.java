package com.example.customerportal.auth.exception;

import com.example.customerportal.common.exception.InvalidParameter;
import lombok.Getter;

import java.util.List;

@Getter
public class ValidationException extends RuntimeException {
    private final List<InvalidParameter> invalidParameters;

    public ValidationException(String message, List<InvalidParameter> invalidParameters) {
        super(message);
        this.invalidParameters = List.copyOf(invalidParameters);
    }
}
