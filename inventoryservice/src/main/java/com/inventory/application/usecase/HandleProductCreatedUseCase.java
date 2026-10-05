package com.inventory.application.usecase;

import org.jboss.logging.Logger;
import com.inventory.domain.model.Inventory;
import com.inventory.domain.repository.InventoryRepository;
import com.inventory.infrastructure.rabbitmq.event.ProductCreatedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class HandleProductCreatedUseCase {

    private static final Logger LOG = Logger.getLogger(HandleProductCreatedUseCase.class);

    @Inject
    InventoryRepository inventoryRepository;

    public void execute(ProductCreatedEvent event) {
        if (event == null || event.id == null) {
            throw new IllegalArgumentException("Invalid product event");
        }
        LOG.infof("HandleProductCreatedUseCase | event.id=%s", event.id);
        Inventory inventory = new Inventory(
                null,
                event.id,
                null,
                0,
                0,
                0);

        inventoryRepository.update(inventory);
    }
}