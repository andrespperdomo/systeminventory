package com.inventory.infrastructure.redis;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.domain.model.InventoryConfirm;
import com.inventory.domain.model.Inventory;
import com.inventory.domain.model.PurchaseHistory;
import com.inventory.domain.model.Status;
import com.inventory.domain.repository.InventoryRepository;

import io.quarkus.arc.Unremovable;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisConnection;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import com.inventory.domain.repository.CacheRedisRepository;
import com.inventory.domain.repository.HistoricalPurchaseRepository;
import com.inventory.shared.utils.ExpireKeyRedisUtil;

@Singleton
@Startup
@Unremovable
public class RedisExpirationListener {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    Redis redis;

    @Inject
    InventoryRepository inventoryRepository;

    @Inject
    CacheRedisRepository cacheRedisRepository;

    @Inject
    HistoricalPurchaseRepository historicalPurchaseRepository;

    @Inject
    ExpireKeyRedisUtil expiredKeyRedisUtil;

    private RedisConnection connection;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public RedisExpirationListener() {
        Log.info("[RedisExpirationListener] Bean constructed");
    }

   /*  @PostConstruct
    void init() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        Log.info("[RedisExpirationListener] Starting initialization");

        redis.connect()
                .onSuccess(conn -> {
                    this.connection = conn;
                    Log.info("[RedisExpirationListener] Connected to Redis for expiration events");

                    conn.handler(this::handleMessage);

                    conn.send(
                            Request.cmd(Command.PSUBSCRIBE)
                                    .arg("__keyevent@0__:expired"))
                            .onSuccess(r -> Log.info(
                                    "[RedisExpirationListener] Subscribed to Redis expiration events on channel __keyevent@0__:expired"))
                            .onFailure(t -> Log.error(
                                    "[RedisExpirationListener] Failed to subscribe to Redis expiration events", t));
                })
                .onFailure(t -> {
                    Log.error(
                            "[RedisExpirationListener] Unable to connect to Redis at startup. The listener will remain inactive until Redis is available",
                            t);
                });
    }*/

    private void handleMessage(Response response) {

      

        Log.infof("[RedisExpirationListener] Received Redis event: %s===========", response);

        if (response == null || response.size() < 4) {
            Log.warn("[RedisExpirationListener] Received incomplete Redis event payload; skipping it");
            return;
        }

        String key = response.get(1).toString();
        Log.infof("[RedisExpirationListener] Processing expiration for PAYLOAD 1======================= : %s", key);

        String expiredKey = response.get(3).toString();
        Log.infof("[RedisExpirationListener] Processing expiration for PAYLOAD 2======================= : %s",
                expiredKey);

       // String metadataKey = "reservationmeta:" + parseProductIdFromKey(expiredKey);
       String metadataKey = expiredKeyRedisUtil.buildMetaKey(expiredKey);
        
       /* if (payload != null) {
            try {
                Map<String, Object> data = OBJECT_MAPPER.readValue(payload, Map.class);
                reserved = Integer.valueOf(data.get("reserved").toString());
            } catch (JsonProcessingException e) {
                Log.errorf(e, "[RedisExpirationListener] Failed to parse payload for key: %s", metadataKey);
            }
        }*/

        

        // Log.infof("[RedisExpirationListener] Received Redis event: pattern=%s
        // channel=%s",
        // response.get(1).toString(), response.get(2).toString());
        String keyHistory =expiredKey.split("\\|")[1];
        CompletableFuture.runAsync(() -> {
            Log.infof("[RedisExpirationListener] Processing expiration for PAYLOAD 3======================= : %s", metadataKey);
        System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& data   " +cacheRedisRepository.find(metadataKey, "data", InventoryConfirm.class));
        InventoryConfirm payload = cacheRedisRepository.find(metadataKey, "data", InventoryConfirm.class)
                .orElse(null);

        Log.infof("[RedisExpirationListener] Processing expiration for PAYLOAD ########======================= : %s",  payload);      
        Log.infof("[RedisExpirationListener] Parsed reserved quantity: %s", payload.reserved());
        Integer reserved = 0;
        Log.infof("[RedisExpirationListener] Processing expiration payload for key %s: %s", metadataKey, payload.reserved());
            processExpiredEvent(metadataKey);
               PurchaseHistory purchaseHistory=new PurchaseHistory(keyHistory,Status.EXPIRED.name());
              historicalPurchaseRepository.update(purchaseHistory);
        
        });

     

        
    }

  @Transactional
public void processExpiredEvent(String expiredKey) {

    Inventory inventory = validateExpirationKey(expiredKey);

    Inventory existingInventory = loadInventory(
            parseProductId(inventory.idProduct()));

    List<ReservationEntry> reservations =
            loadReservations(inventory.idProduct());

    int releasedQuantity =
            calculateReleasedQuantity(reservations);

    Inventory updatedInventory =
            buildUpdatedInventory(
                    existingInventory,
                    releasedQuantity);

    persistInventory(updatedInventory);

    cleanupReservations(
           Long.parseLong(existingInventory.idProduct()) ,
            reservations);

    logInventoryUpdate(
            updatedInventory,
            releasedQuantity);
}

private Inventory validateExpirationKey(String expiredKey) {

    return parseInventoryFromExpiredKey(expiredKey)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Invalid expiration key: "
                                    + expiredKey));
}

private Inventory loadInventory(Long productId) {

    Log.infof(
            "Loading inventory for product %s",
            productId);

    return inventoryRepository
            .findById(productId)
            .orElseThrow(() ->
                    new IllegalStateException(
                            "Inventory not found for product "
                                    + productId));
}

private List<ReservationEntry> loadReservations(
        String productId) {

    List<ReservationEntry> reservations =
            cacheRedisRepository.getReserved(
                    productId,
                    "data");

    reservations.forEach(System.out::println);               

    if (reservations.isEmpty()) {

        Log.infof(
                "No reservations found for product %s",
                productId);
    }

    return reservations;
}
private int calculateReleasedQuantity(
        List<ReservationEntry> reservations) {

    int reservation = reservations.stream()

            .map(ReservationEntry::data)

            .map(data -> data.get("reserved"))

            .filter(Number.class::isInstance)

            .map(Number.class::cast)

            .mapToInt(Number::intValue)

            .sum();
            Log.infof("[RedisExpirationListener] loadReservations: %s", reservation);
    return reservation;        
}

private Inventory buildUpdatedInventory(
        Inventory inventory,
        int releasedQuantity) {

    int reserved =
            inventory.reserved() - releasedQuantity;
   System.out.println("----->>>>>>>>>>>>>>>>reserved1"+reserved);       

    int available =
            inventory.available() + releasedQuantity;
    System.out.println("----->>>>>>>>>>>>>>>>available1"+available);           

    if (reserved < 0) {

        throw new IllegalStateException(
                "Reserved inventory cannot be negative");
    }

    if (available > inventory.quantity()) {

        throw new IllegalStateException(
                "Available inventory exceeds quantity");
    }
    System.out.println("----->>>>>>>>>>>>>>>>reserved"+reserved);
      System.out.println("----->>>>>>>>>>>>>>>>Availabe"+available);

    return new Inventory(

            inventory.id(),

            inventory.idProduct(),

            inventory.idUser(),

            reserved,

            available,

            inventory.quantity());
}

private void persistInventory(
        Inventory inventory) {

    inventoryRepository.update(inventory);
}

private void cleanupReservations(

        Long productId,

        List<ReservationEntry> reservations) {

    for (ReservationEntry reservation : reservations) {

        try {

            cacheRedisRepository.delete(
                    reservation.redisKey(),
                    "data");

       //7     cacheRedisRepository.removeProductIndex(
          //          productId,
            //        reservation.redisKey());

        } catch (Exception e) {

            Log.errorf(
                    e,
                    "Unable to remove reservation %s",
                    reservation.redisKey());
        }
    }
}

private void logInventoryUpdate(

        Inventory inventory,

        int released) {

    Log.infof(
            """
            Inventory updated successfully

            Product      : %s
            Released     : %d
            Reserved     : %d
            Available    : %d
            Quantity     : %d
            """,

            inventory.idProduct(),

            released,

            inventory.reserved(),

            inventory.available(),

            inventory.quantity());
}

public void onExpired(String key){

   // reservationExpirationService.process(key);

}

    static Optional<Inventory> parseInventoryFromExpiredKey(String payload) {
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }

        String trimmedPayload = payload.trim();

        if (trimmedPayload.startsWith("{") || trimmedPayload.startsWith("[")) {
            try {
                Inventory inventory = OBJECT_MAPPER.readValue(trimmedPayload, Inventory.class);
                return Optional.ofNullable(inventory);
            } catch (JsonProcessingException ex) {
                Log.warnf("[RedisExpirationListener] Unable to parse JSON payload: %s", trimmedPayload);
            }
        }

        String[] parts = trimmedPayload.split(":");
        if (parts.length < 2 || !"reservationmeta".equals(parts[0])) {
            return Optional.empty();
        }
        String [] product = parts[1].trim().split("|");
        String productId = product[0].trim();
        if (productId.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(Inventory.withReserved(productId, null, 0, 0));
    }


    private Long parseProductId(String productId) {
        try {
            return Long.valueOf(productId);
        } catch (NumberFormatException ex) {
            Log.errorf(ex, "[RedisExpirationListener] Invalid product id for Redis expiration event: %s", productId);
            throw new IllegalArgumentException("Invalid product id for Redis expiration event: " + productId, ex);
        }
    }

    private String parseProductIdFromKey(String expiredKey) {
        if (expiredKey == null || expiredKey.isBlank()) {
            return "";
        }

        String[] parts = expiredKey.split(":");
        return parts.length > 1 ? parts[1] : "";
    }

    @PreDestroy
    void shutdown() {
        if (connection != null) {
            Log.info("[RedisExpirationListener] Closing Redis connection");
            connection.close();
        }
    }
}