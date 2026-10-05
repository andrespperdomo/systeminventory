package com.inventory.domain.repository;

import java.util.Optional;

import com.inventory.domain.model.Inventory;

public interface InventoryRepository {

    Inventory update(Inventory product);

    Optional<Inventory> findById(Long id);

    // public PageResult<Inventory> findAllPage(String search, int page, int size);

}
