package com.inventory.domain.repository;

import java.util.Optional;
import com.inventory.domain.model.Inventory;

public interface ReservationRepository {

    void reserve(Inventory inventory);

    Optional<Integer> getReservation(String productId, String userId);

    void remove(String productId, String userId);

    void removeByKey(String key);

}
