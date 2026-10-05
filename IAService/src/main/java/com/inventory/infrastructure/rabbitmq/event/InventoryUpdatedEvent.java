package com.inventory.infrastructure.rabbitmq.event;

import java.math.BigDecimal;

public record InventoryUpdatedEvent(
                String productId,
                BigDecimal quantity,
                String type) {
}