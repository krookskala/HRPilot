package com.hrpilot.backend.common.exception;

public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
    super(message);
    }
}