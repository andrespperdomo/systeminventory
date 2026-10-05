package com.inventory.infrastructure.rabbitmq.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationEvent(
                String reservationId,
                String productId,
                Integer quantity,
                BigDecimal unitPrice,
                String reservedAt,
                String type) {
}