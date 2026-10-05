package com.inventory.application.command;

import java.time.LocalDateTime;

public record ReservationTrackingCommand(
        String reservationId,
        String productId,
        int userId,
        int quantity,
        LocalDateTime reservedAt

) {
    
}
