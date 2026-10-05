package com.inventory.domain.exception;

public class RedisNotFoundException extends RuntimeException {
    public RedisNotFoundException(String productId, String userId) {
        super("the redis inventory not found for product: " + productId + " and user: " + userId);
    }
}
