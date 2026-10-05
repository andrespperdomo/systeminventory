package com.inventory.domain.model;

import java.time.LocalDateTime;
import com.inventory.infrastructure.persistence.PurchaseHistoryEntity;

public class HistoricalPurchaseMapper {

    public static PurchaseHistoryEntity toEntity(PurchaseHistory purchaseHistory) {
        PurchaseHistoryEntity e = new PurchaseHistoryEntity();
        e.idUser = purchaseHistory.idUser();
        e.productId = purchaseHistory.productId();
        e.quantity = purchaseHistory.quantity();
        e.type = purchaseHistory.type();
        e.createdAt = LocalDateTime.now();
        return e;
    }

    public static PurchaseHistory toDomain(PurchaseHistoryEntity e) {
        return new PurchaseHistory(e.reservationId, e.productId, e.quantity, e.type, e.idUser);
    }

}
