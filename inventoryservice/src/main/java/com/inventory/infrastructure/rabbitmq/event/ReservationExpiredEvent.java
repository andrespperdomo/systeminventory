package com.inventory.infrastructure.rabbitmq.event;

public record ReservationExpiredEvent(
        String idProduct,
        Integer reserved) {
}