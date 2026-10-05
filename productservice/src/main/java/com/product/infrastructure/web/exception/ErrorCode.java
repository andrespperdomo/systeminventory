package com.product.infrastructure.web.exception;

public enum ErrorCode {
    INVENTORY_NOT_FOUND("INVENTORY_NOT_FOUND"),
    INSUFFICIENT_RESOURCES("INSUFFICIENT_RESOURCES"),
    INSUFFICIENT_RESOURCES_AVAILABLE("INSUFFICIENT_RESOURCES_AVAILABLE"),
    REDIS_RESOURCES_AVAILABLE("REDIS_RESOURCES_AVAILABLE"),
    HISTORICAL_RESOURCES_AVAILABLE("HISTORICAL_RESOURCES_AVAILABLE"),
    INTERNAL_ERROR("INTERNAL_ERROR");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
