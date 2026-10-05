package com.inventory.infrastructure.redis;

import java.util.Optional;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.domain.repository.ReservationRepository;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.inventory.infrastructure.logging.LogMethod;
import com.inventory.domain.model.Inventory;

@ApplicationScoped
@LogMethod
public class ReservationRedisRepository implements ReservationRepository {

    private static final Logger LOG = Logger.getLogger(ReservationRedisRepository.class);

    private final RedisDataSource redisDataSource;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    public ReservationRedisRepository(RedisDataSource redisDataSource) {
        this.redisDataSource = redisDataSource;
    }

    @Override
    public void reserve(Inventory inventory) {
        if (inventory.idProduct() == null || inventory.idProduct().isBlank() || inventory.idUser() == null
                || inventory.idUser().isBlank() || inventory.reserved() == null
                || inventory.reserved() <= 0) {
            LOG.warnf("Invalid reservation parameters: productId=%s userId=%s quantity=%s", inventory.idProduct(),
                    inventory.idUser(),
                    inventory.reserved());
            return;
        }

        String key = "reservation:" + inventory.idProduct();

        LOG.infof("Starting Redis reservation | productId=%s | userId=%s | quantity=%s", inventory.idProduct(),
                inventory.idUser(), inventory.reserved());

        try {
            String json = objectMapper.writeValueAsString(inventory);

            var hash = redisDataSource.hash(String.class, String.class, String.class);
            // Save reservation
            hash.hset(key, inventory.idUser(), json);

            LOG.infof("Reservation stored in Redis | key=%s | field=%s | value=%s", key, inventory.idUser(),
                    inventory.reserved());

            // Set expiration (15 minutes)
            redisDataSource.key().expire(key, 900);

            LOG.infof("TTL applied successfully | key=%s | ttlSeconds=%s", key, 900);
        } catch (Exception e) {
            LOG.errorf(e, "Redis reservation failed | productId=%s | userId=%s", inventory.idProduct(),
                    inventory.idUser());
        }
    }

    @Override
    public Optional<Integer> getReservation(String productId, String userId) {
        if (productId == null || productId.isBlank()) {
            LOG.warn("productId is null or empty");
            return Optional.empty();
        }

        if (userId == null || userId.isBlank()) {
            LOG.warn("userId is null or empty");
            return Optional.empty();
        }

        String key = "reservation:" + productId;

        LOG.infof("Redis key=%s userId=%s", key, userId);

        try {
            HashCommands<String, String, String> hash = redisDataSource.hash(String.class);
            String value = hash.hget(key, userId);

            LOG.infof("Redis================================== value=%s", value);

            if (value == null || value.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            LOG.errorf("Invalid integer value in Redis for productId=%s userId=%s", productId, userId);
            return Optional.empty();
        } catch (Exception e) {
            LOG.error("Redis error while getting reservation", e);
            return Optional.empty();
        }
    }

    @Override
    public void remove(String productId, String userId) {
        if (productId == null || productId.isBlank() || userId == null || userId.isBlank()) {
            LOG.warnf("Invalid remove parameters: productId=%s userId=%s", productId, userId);
            return;
        }

        String key = "reservation:" + productId;

        try {
            HashCommands<String, String, String> hash = redisDataSource.hash(String.class);
            hash.hdel(key, userId);
            LOG.infof("Reservation removed | key=%s | field=%s", key, userId);
        } catch (Exception e) {
            LOG.errorf(e, "Redis error while removing reservation | productId=%s | userId=%s", productId, userId);
        }
    }

    @Override
    public void removeByKey(String key) {

        if (key == null || key.isBlank()) {
            LOG.warn("Invalid Redis key");
            return;
        }

        try {
            redisDataSource.key().del(key);

            LOG.infof("Redis key removed | key=%s", key);

        } catch (Exception e) {
            LOG.errorf(e, "Error removing Redis key | key=%s", key);
        }
    }


}