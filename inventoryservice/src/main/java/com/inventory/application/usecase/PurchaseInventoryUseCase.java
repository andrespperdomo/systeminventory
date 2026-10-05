package com.inventory.application.usecase;

import java.math.BigDecimal;

import org.apache.kafka.common.security.authenticator.CredentialCache.Cache;
import org.jboss.logging.Logger;

import com.inventory.application.command.PurchaseInventoryCommand;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.inventory.application.command.ReservationCommand;
import com.inventory.application.command.ReservationTrackingCommand;
import com.inventory.application.command.SaveHistoricalPurchaseCommand;
import com.inventory.application.service.OutboxEventPublisher;
import com.inventory.domain.event.EventType;
import com.inventory.domain.exception.InsufficientAvailableProductException;
import com.inventory.domain.exception.InsufficientStockException;
import com.inventory.domain.exception.InventoryNotFoundException;
import com.inventory.domain.model.Inventory;
import com.inventory.domain.repository.InventoryRepository;
import com.inventory.domain.repository.OutboxRepository;
import com.inventory.domain.repository.CacheRedisRepository;
import com.inventory.infrastructure.persistence.InventoryProductEntity;
import com.inventory.infrastructure.rabbitmq.event.InventoryConfirmEvent;
import com.inventory.infrastructure.rabbitmq.event.ReservationEvent;
import com.inventory.infrastructure.redis.ReservationRedisRepository;
import com.inventory.shared.utils.ExpireKeyRedisUtil;
import com.inventory.application.usecase.SaveHistoricalPurchase;
import com.inventory.domain.exception.SaveHistoricalPurchaseException;
import com.inventory.domain.model.InventoryConfirm;
import com.inventory.domain.model.Status;
import com.inventory.infrastructure.persistence.InventoryProductRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PurchaseInventoryUseCase {

    private static final Logger LOG = Logger.getLogger(PurchaseInventoryUseCase.class);

    private final InventoryRepository repository;

    private final ReservationRedisRepository reservationRedisRepository;

    private final CacheRedisRepository cacheRedisRepository;

    private final OutboxEventPublisher outboxEventPublisher;

    private final SaveHistoricalPurchase saveHistoricalPurchase;

    private final ExpireKeyRedisUtil expiredKeyRedisUtil;

    private final ReservationUseCase reservationUseCase;

    private final InventoryProductRepository inventoryProductRepository; 

    private final ReservationTrackingUseCase reservationTrackingUseCase;

    @Inject
    public PurchaseInventoryUseCase(InventoryRepository productRepository,
            ReservationRedisRepository reservationRedisRepository,
            OutboxEventPublisher outboxEventPublisher, CacheRedisRepository cacheRedisRepository,
            SaveHistoricalPurchase saveHistoricalPurchase, ExpireKeyRedisUtil expiredKeyRedisUtil,
        ReservationUseCase reservationUseCase, InventoryProductRepository inventoryProductRepository,
        ReservationTrackingUseCase reservationTrackingUseCase) {
        this.repository = productRepository;
        this.reservationRedisRepository = reservationRedisRepository;
        this.outboxEventPublisher = outboxEventPublisher;
        this.cacheRedisRepository = cacheRedisRepository;
        this.saveHistoricalPurchase = saveHistoricalPurchase;
        this.expiredKeyRedisUtil = expiredKeyRedisUtil;
        this.reservationUseCase = reservationUseCase;
        this.inventoryProductRepository=inventoryProductRepository;
        this.reservationTrackingUseCase = reservationTrackingUseCase;
    }

    public Inventory execute(PurchaseInventoryCommand command) {
        LOG.infof("Inventory updated | productId=%s", command.idProduct());
        // 1. Build domain object (NO ID from command)
        Inventory inventory = repository.findById(Long.valueOf(command.idProduct()))
                .orElseThrow(() -> new InventoryNotFoundException(command.idProduct()));

        if (inventory.quantity() == 0 || inventory.quantity() == null) {
            throw new InsufficientStockException(0, command.reserved());
        }

        int reserved = inventory.reserved();
        int reservedBD = reserved;
        reserved = command.reserved();
        reservedBD += command.reserved();
        if (inventory.quantity() < reserved) {
            throw new InsufficientAvailableProductException(reserved, inventory.quantity());
        }
        if (inventory.quantity() < reservedBD) {
            throw new InsufficientAvailableProductException(reservedBD, inventory.quantity());
        }

        // business logic INSIDE domain
        inventory.decrease(inventory.quantity());

        int available = inventory.quantity() - reservedBD;

        inventory = new Inventory(inventory.id(), inventory.idProduct(), inventory.idUser(), reservedBD, available,
                inventory.quantity());

        // reservationRedisRepository.reserve(
        // inventory);
        String key = UUID.randomUUID().toString();
        String reservationKey = expiredKeyRedisUtil.buildKey(command.idProduct(), key);
        String reservationMetaKey = expiredKeyRedisUtil.buildMetaKey(reservationKey);
        // String payload =
        // String.format("{\"userId\":\"%s\",\"reserved\":\"%s\",\"createDate\":%s}",
        // command.idUser(), command.reserved(),LocalDateTime.now().toString());
        InventoryConfirm inventoryConfirm = new InventoryConfirm(command.idUser(), inventory.quantity(), reserved);
        cacheRedisRepository.save(reservationKey, "data", inventoryConfirm, Duration.ofSeconds(80));
        // System.out.println("()()(()()()()()()()()()()()()()()()()()()()(
        // data"+cacheRedisRepository.find(reservationKey, "data", String.class));
        cacheRedisRepository.save(reservationMetaKey, "data", inventoryConfirm, null);
        System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& data "
                + cacheRedisRepository.find(reservationMetaKey, "data", InventoryConfirm.class));
        cacheRedisRepository.saveProductIndex(inventory.idProduct(), reservationMetaKey);
        // 3. Persist updated state
        Inventory updated = repository.update(inventory);

 try {
        ReservationCommand reservationCommand =new ReservationCommand(key,
             command.idProduct(),
          inventory.quantity(), 
           command.idUser());
        reservationUseCase.execute(reservationCommand);


        ReservationTrackingCommand reservationTrackingCommand = new ReservationTrackingCommand(key,
                command.idProduct(), reserved, LocalDateTime.now());


        reservationTrackingUseCase.execute(reservationTrackingCommand);
        

        ReservationEvent event = buildEvent(command, key);

        // 4. Save OUTBOX EVENT
        outboxEventPublisher.publish(command.idProduct(), EventType.RESERVATION_CREATED.name(), event);

       
            SaveHistoricalPurchaseCommand saveHistoricalPurchaseCommand = new SaveHistoricalPurchaseCommand(key,
                    command.idProduct(), reserved, Status.PENDING.name(), command.idUser());

            saveHistoricalPurchase.execute(saveHistoricalPurchaseCommand);
        } catch (Exception e) {
            throw new SaveHistoricalPurchaseException(
                    "Failed to save purchase history",
                    e);
        }

        // (command, updated);
        return updated;

    }

    // =========================
    // OUTBOX EVENT CREATION
    // =========================
    /*
     * private void saveOutboxEvent(PurchaseInventoryCommand command, Inventory
     * inventory) {
     * 
     * InventoryUpdatedEvent event = buildEvent(command);
     * 
     * EventEnvelope<InventoryUpdatedEvent> envelope = new
     * EventEnvelope<>(EventType.INVENTORY_CREATED.name(),
     * event);
     * 
     * outboxRepository.save(new OutboxEvent(
     * Long.parseLong(event.productId()),
     * event.productId().toString(),
     * EventType.INVENTORY_CREATED.name(),
     * JsonUtil.toJson(envelope),
     * Status.PENDING.name(), 0));
     * 
     * }
     */

    // =========================
    // EVENT BUILDER
    // =========================

    private ReservationEvent buildEvent(PurchaseInventoryCommand command, String idReservation) {
        InventoryProductEntity product =
        inventoryProductRepository.findByProductId(command.idProduct())
                .orElseThrow();
         BigDecimal unitPrice = product.getUnitPrice();      
        return new ReservationEvent(
                idReservation,
                command.idProduct(),
                command.reserved(),
                unitPrice,
                LocalDateTime.now().toString(),
                "STOCK_DECREASE");


    }

}
