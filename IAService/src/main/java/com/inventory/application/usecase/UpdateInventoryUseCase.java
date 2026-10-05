package com.inventory.application.usecase;

import java.math.BigDecimal;

import org.jboss.logging.Logger;
import com.inventory.application.command.UpdateInventoryCommand;
import com.inventory.domain.exception.InventoryNotFoundException;
import com.inventory.domain.model.Inventory;
import com.inventory.domain.outbox.OutboxEvent;
import com.inventory.domain.outbox.OutboxStatus;
import com.inventory.domain.outbox.OutboxType;
import com.inventory.domain.repository.InventoryRepository;
import com.inventory.domain.repository.OutboxRepository;
import com.inventory.infrastructure.rabbitmq.event.InventoryUpdatedEvent;

import com.inventory.shared.utils.JsonUtil;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UpdateInventoryUseCase {

    private static final Logger LOG = Logger.getLogger(UpdateInventoryUseCase.class);

    private final InventoryRepository repository;

    private final OutboxRepository outboxRepository;

    @Inject
    public UpdateInventoryUseCase(InventoryRepository productRepository, OutboxRepository outboxRepository) {
        this.repository = productRepository;
        this.outboxRepository = outboxRepository;
    }

    public Inventory execute(UpdateInventoryCommand command) {
        LOG.infof("Inventory updated | productId=%s quantity=%s", command.idProduct(),
                String.valueOf(command.quantity()));
        // 1. Build domain object (NO ID from command)
        Inventory inventory = repository.findById(Long.valueOf(command.idProduct()))
                .orElseThrow(() -> new InventoryNotFoundException(command.idProduct()));

        // business logic INSIDE domain
        // inventory.decrease(command.quantity());

        // 3. Persist updated state

        inventory = inventory.addStock(command.quantity());
        Inventory updated = repository.update(inventory);
        Log.info("###################################### " + updated.quantity() + "/" + updated.idProduct());

        // 4. Save OUTBOX EVENT
        saveOutboxEvent(command);
        return updated;

    }

    // =========================
    // OUTBOX EVENT CREATION
    // =========================
    private void saveOutboxEvent(UpdateInventoryCommand command) {

        InventoryUpdatedEvent event = buildEvent(command);

        OutboxEvent outboxEvent = new OutboxEvent(Long.parseLong(command.idProduct()), command.idProduct(),
                OutboxType.INVENTORY_UPDATED.name(),
                JsonUtil.toJson(event),
                OutboxStatus.PENDING.name(), 0);

        outboxRepository.save(outboxEvent);
    }

    // =========================
    // EVENT BUILDER
    // =========================
     private InventoryUpdatedEvent buildEvent(UpdateInventoryCommand command) {
        return new InventoryUpdatedEvent(
                command.idProduct(),
                BigDecimal.valueOf(command.quantity()),
                "STOCK_DECREASE");
    }

}
