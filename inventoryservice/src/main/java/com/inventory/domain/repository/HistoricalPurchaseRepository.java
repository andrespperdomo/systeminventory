package com.inventory.domain.repository;

import java.util.Optional;
import com.inventory.domain.model.PurchaseHistory;

public interface HistoricalPurchaseRepository {

    PurchaseHistory save(PurchaseHistory purchaseHistory);

    PurchaseHistory update(PurchaseHistory purchaseHistory);

    Optional<PurchaseHistory> findById(Long id);
}
