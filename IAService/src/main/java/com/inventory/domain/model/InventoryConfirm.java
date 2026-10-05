package com.inventory.domain.model;

import java.time.LocalDateTime;

public record InventoryConfirm(
        String userId,
        int reserved,
        LocalDateTime createDate) {

    public InventoryConfirm(String userId, int reserved) {
        this(userId, reserved, LocalDateTime.now());
    }
}
