package com.inventory.shared.utils.constants;


public enum RedisKeyPrefix {

    RESERVATION("reservation:"),
    RESERVATION_META("reservationmeta:"),
    PRODUCT_INDEX("product-index:");

    
    private final String value;

    RedisKeyPrefix(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
