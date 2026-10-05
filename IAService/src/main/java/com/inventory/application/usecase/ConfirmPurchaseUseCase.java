package com.inventory.application.usecase;

import org.jboss.logging.Logger;

import com.inventory.application.command.ConfirmPurchaseCommand;
import com.inventory.application.service.OutboxEventPublisher;
import com.inventory.domain.exception.InsufficientStockException;
import com.inventory.domain.exception.InventoryNotFoundException;
import com.inventory.domain.exception.RedisNotFoundException;
import com.inventory.domain.model.Inventory;
import com.inventory.domain.model.InventoryConfirm;
import com.inventory.domain.model.PurchaseHistory;
import com.inventory.domain.model.Status;
import com.inventory.domain.repository.CacheRedisRepository;
import com.inventory.domain.repository.HistoricalPurchaseRepository;
import com.inventory.domain.repository.InventoryRepository;
import com.inventory.infrastructure.rabbitmq.event.InventoryConfirmEvent;
import com.inventory.infrastructure.redis.ReservationRedisRepository;
import com.inventory.shared.utils.JsonUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConfirmPurchaseUseCase {

    private static final Logger LOG = Logger.getLogger(ConfirmPurchaseUseCase.class);

    private final InventoryRepository inventoryRepository;
    private final ReservationRedisRepository reservationRedisRepository;
    private final CacheRedisRepository cacheRedisRepository;
    private final HistoricalPurchaseRepository purchaseRepository;
    private final OutboxEventPublisher eventPublisher;

    @Inject
    public ConfirmPurchaseUseCase(
            InventoryRepository inventoryRepository,
            ReservationRedisRepository reservationRedisRepository,
            CacheRedisRepository cacheRedisRepository,
            HistoricalPurchaseRepository purchaseRepository,
            OutboxEventPublisher eventPublisher) {

        this.inventoryRepository = inventoryRepository;
        this.reservationRedisRepository = reservationRedisRepository;
        this.cacheRedisRepository = cacheRedisRepository;
        this.purchaseRepository = purchaseRepository;
        this.eventPublisher = eventPublisher;
    }

    public Inventory execute(ConfirmPurchaseCommand command) {

        LOG.infof("Confirming purchase reservationId=%s", command.idReservation());

        Long productId = Long.parseLong(command.idProduct());

        Integer reservedQuantity = getReservedQuantity(command);

        Inventory inventory = loadInventory(productId);

        validateStock(inventory, reservedQuantity);

        Inventory updatedInventory = confirmInventory(inventory, reservedQuantity);

        removeReservation(command);

        updatePurchaseHistory(command);

      //  publishConfirmationEvent(command);

        return updatedInventory;
    }

    private Integer getReservedQuantity(ConfirmPurchaseCommand command) {

        String key = buildReservationKey(command);

        InventoryConfirm payload = cacheRedisRepository.find(key, "data", InventoryConfirm.class)
                .orElseThrow(() -> new RedisNotFoundException(key, key));
        LOG.infof("Confirming purchase | getReservedQuantity reservationId=%s", payload.reserved());        

        return payload.reserved();
    }

    private Inventory loadInventory(Long productId) {
       LOG.infof("Confirming purchase | loadInventory productId=%s", productId);   
        return inventoryRepository.findById(productId)
                .orElseThrow(() ->
                        new InventoryNotFoundException(productId.toString()));
    }

    private void validateStock(Inventory inventory, Integer reserved) {
      LOG.infof("Confirming purchase | validateStock quantity=%s reserved=%s", inventory.quantity(), reserved);  
        if (inventory.quantity() < reserved) {
            throw new InsufficientStockException(
                    inventory.quantity(),
                    reserved);
        }
    }

    private Inventory confirmInventory(Inventory inventory, Integer reserved) {

        Inventory updated = inventory.decrease(reserved);

        updated = new Inventory(
                updated.id(),
                updated.idProduct(),
                updated.idUser(),
                updated.reserved() - reserved,
                updated.available(),
                updated.quantity());
        LOG.infof("Confirming purchase | confirmInventory update quantity=%s", updated.quantity()); 
        return inventoryRepository.update(updated);
    }

    private void removeReservation(ConfirmPurchaseCommand command) {
        LOG.infof("Confirming purchase | removeReservation idProduct=%s", command.idProduct()); 

        reservationRedisRepository.remove(
                command.idProduct(),
                command.idUser());

        //   reservationRedisRepository.removeByKey(buildReservationKey(command));     

    }

    private void updatePurchaseHistory(ConfirmPurchaseCommand command) {
        LOG.infof("Confirming purchase | updatePurchaseHistory idReservation=%s", command.idReservation()); 

        PurchaseHistory purchaseHistory =
                new PurchaseHistory(
                        command.idReservation(),
                        Status.PURCHASE.name());

        purchaseRepository.update(purchaseHistory);
    }



    private String buildReservationKey(ConfirmPurchaseCommand command) {

        return "reservation:%s|%s"
                .formatted(command.idProduct(), command.idReservation());
    }
}