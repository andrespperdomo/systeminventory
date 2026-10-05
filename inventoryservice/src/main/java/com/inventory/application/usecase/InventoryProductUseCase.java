package com.inventory.application.usecase;

import com.inventory.infrastructure.persistence.InventoryProductEntity;
import com.inventory.infrastructure.persistence.InventoryProductRepository;
import com.inventory.application.command.InventoryProductCommand;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InventoryProductUseCase {


    @Inject
    InventoryProductRepository repository;

    @Transactional
    public void execute(InventoryProductCommand event) {

        InventoryProductEntity entity = repository
                .findByProductId(event.productId())
                .orElse(null);

        if (entity == null) {

            entity = InventoryProductEntity.builder()
                    .productId(event.productId())
                    .unitPrice(event.unitPrice())
                    .build();

            repository.persist(entity);

        } else {

            // Product already exists.
            // Update the price if this event represents the current price.
            entity.setUnitPrice(event.unitPrice());
        }
    }
    
}
