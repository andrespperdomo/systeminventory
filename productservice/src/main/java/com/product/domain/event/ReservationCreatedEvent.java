package com.product.domain.event;


import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationCreatedEvent(

    Long productId,

    String reservationId,

    String userId,

    Integer quantity,

    BigDecimal unitPrice,

    String reservedAt

) {
}