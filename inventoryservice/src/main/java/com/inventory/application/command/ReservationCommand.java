package com.inventory.application.command;

public record ReservationCommand(
    String reservationId,
    String productId,
    Integer quantity,
    String userId

) {
}
