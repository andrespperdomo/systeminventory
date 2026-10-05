package com.inventory.domain.model;

import java.time.LocalDateTime;

public record InventoryConfirm(
        String userId,
        int reserved,
        int quantity,
        LocalDateTime createDate) {

    public InventoryConfirm(String userId, int quantity, int reserved) {
        this(userId, reserved, quantity, LocalDateTime.now());
    }
}
