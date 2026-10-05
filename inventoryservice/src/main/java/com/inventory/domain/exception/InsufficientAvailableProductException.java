package com.inventory.domain.exception;

public class InsufficientAvailableProductException extends RuntimeException {
    public InsufficientAvailableProductException(int available, int requested) {
        super("Insufficient stock available overcome to quantity, Available: " + available + ", Quantity: "
                + requested);
    }
}
