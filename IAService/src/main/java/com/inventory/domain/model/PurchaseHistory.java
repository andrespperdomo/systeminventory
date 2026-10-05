package com.inventory.domain.model;

import java.time.LocalDateTime;
import com.inventory.domain.model.Status;

public record PurchaseHistory(
    Long id,
    String reservationId,
    String productId,
    Integer quantity,
    String type,
    String idUser
) {

    public PurchaseHistory(
         String reservationId,
            String productId,
            Integer quantity,
            String type,
            String idUser) {
        this(null, reservationId, productId, quantity, type, idUser);
    }

     public PurchaseHistory(
            String reservationId,
        String type) {
        this(null, reservationId, null, null, type, null);
    }
}
