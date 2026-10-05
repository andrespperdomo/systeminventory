package com.inventory.infrastructure.web.exception;

public class ApiError {

    public String code;
    public String message;

    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }
}