package com.inventory.infrastructure.persistence;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class InventoryProductRepository
        implements PanacheRepository<InventoryProductEntity> {

    public Optional<InventoryProductEntity> findByProductId(
            String productId) {

        return find("productId", productId)
                .firstResultOptional();
    }
}