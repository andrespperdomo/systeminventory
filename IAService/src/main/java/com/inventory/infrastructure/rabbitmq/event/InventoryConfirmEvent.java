package com.inventory.infrastructure.rabbitmq.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryConfirmEvent(
        String reservationId,
        String productId,
        Integer quantity,
        BigDecimal unitPrice,
        String purchasedAt
) {}