package com.inventory.infrastructure.redis;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

import com.inventory.application.usecase.ReservationExpiredUseCase;



import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisConnection;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Response;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.Startup;
import io.vertx.mutiny.redis.client.Command;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Startup
@Unremovable
public class RedisExpirationListenerRefactor {

    private static final Logger LOG =
            Logger.getLogger(RedisExpirationListenerRefactor.class);

    private static final String EXPIRED_CHANNEL =
            "__keyevent@0__:expired";

    @Inject
    Redis redis;

    @Inject
    ReservationExpiredUseCase reservationExpiredUseCase;

    private RedisConnection connection;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Dedicated executor for expiration processing.
     */
    private final ExecutorService executor =
            Executors.newFixedThreadPool(2);

    @PostConstruct
    void init() {

        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        LOG.info("Initializing Redis expiration listener...");
redis.connect()
    .subscribe()
    .with(
        this::onConnected,
        t -> LOG.error("Unable to connect", t)
    );
    }



    private void onConnected(RedisConnection conn) {

    this.connection = conn;

    connection.handler(this::handleMessage);

    connection.send(
            Request.cmd(Command.PSUBSCRIBE)
                    .arg("__keyevent@0__:expired"))
            .subscribe()
            .with(
                r -> LOG.info("Subscribed"),
                t -> LOG.error("Subscribe failed", t)
            );
}

    /**
     * Called by Vert.x every time Redis publishes an expiration event.
     */
    private void handleMessage(Response response) {

     response.forEach(item -> LOG.infof("Redis response item: %s", item));

          LOG.infof("REDIS_EXPIRATION_RAW response=%s", response);
        if (!isValidMessage(response)) {
            LOG.warn("Ignoring malformed Redis expiration event.");
            return;
        }

        String expiredKey = response.get(3).toString();

        LOG.infof("Expired Redis key received: %s", expiredKey);

        executor.submit(() -> processExpiration(expiredKey));
    }

    private void processExpiration(String expiredKey) {

        try {

            reservationExpiredUseCase.execute(expiredKey);

            LOG.infof(
                    "Expiration processed successfully: %s",
                    expiredKey);

        } catch (Exception e) {

            LOG.errorf(
                    e,
                    "Error processing expired key: %s",
                    expiredKey);

        }
    }

    private boolean isValidMessage(Response response) {

        return response != null
                && response.size() >= 4
                && response.get(3) != null;
    }

    @PreDestroy
    void shutdown() {

        LOG.info("Stopping Redis expiration listener...");

        try {

            executor.shutdown();

        } catch (Exception e) {

            LOG.warn("Error shutting down executor.", e);

        }

        if (connection != null) {

            try {

                connection.close();

            } catch (Exception e) {

                LOG.warn("Error closing Redis connection.", e);

            }

        }
    }

}