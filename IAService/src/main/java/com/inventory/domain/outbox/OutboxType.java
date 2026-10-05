package com.inventory.domain.outbox;

public enum OutboxType {
    INVENTORY_UPDATED,
    PRODUCT_CREATED,
    PAYMENT_COMPLETED
}