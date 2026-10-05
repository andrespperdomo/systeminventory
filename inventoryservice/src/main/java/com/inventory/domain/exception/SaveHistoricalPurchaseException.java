package com.inventory.domain.exception;

public class SaveHistoricalPurchaseException extends RuntimeException {

    public SaveHistoricalPurchaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
