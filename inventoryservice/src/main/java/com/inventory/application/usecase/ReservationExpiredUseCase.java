package com.inventory.application.usecase;

import org.jboss.logging.Logger;

import com.inventory.application.service.OutboxEventPublisher;
import com.inventory.domain.event.EventType;
import com.inventory.domain.model.InventoryConfirm;
import com.inventory.domain.model.PurchaseHistory;
import com.inventory.domain.model.Status;
import com.inventory.domain.repository.CacheRedisRepository;
import com.inventory.domain.repository.HistoricalPurchaseRepository;
import com.inventory.infrastructure.rabbitmq.event.ReservationEvent;
import com.inventory.infrastructure.rabbitmq.event.ReservationExpiredEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.inventory.domain.repository.CacheRedisRepository;
import com.inventory.shared.utils.ExpireKeyRedisUtil;
import com.inventory.shared.utils.ExpireRedisUtil;

@ApplicationScoped
public class ReservationExpiredUseCase {

        private static final Logger LOG = Logger.getLogger(ReservationExpiredUseCase.class);

        @Inject
        CacheRedisRepository cacheRedisRepository;

        @Inject
        HistoricalPurchaseRepository historicalPurchaseRepository;

        @Inject
        ExpireRedisUtil expireRedisUtil;

        @Inject
        ExpireKeyRedisUtil expireKeyRedisUtil;

        @Inject
        ReleaseReservationUseCase releaseReservationUseCase;

        @Inject
        private OutboxEventPublisher outboxEventPublisher;

        @Transactional
        public void execute(String expiredKey) {

                LOG.infof("Processing expired reservation: %s", expiredKey);

                String metadataKey = expireKeyRedisUtil.buildMetaKey(expiredKey);

                InventoryConfirm reservation = cacheRedisRepository

                                .find(
                                                metadataKey,
                                                "data",
                                                InventoryConfirm.class)

                                .orElseThrow(() -> new IllegalStateException(
                                                "Reservation metadata not found: "
                                                                + metadataKey));

                LOG.infof("Reservation recovered: %s", reservation);

                releaseReservationUseCase.execute(expiredKey, reservation);
                cacheRedisRepository.delete(metadataKey, "data");

                String reservationId = expiredKey.split("\\|")[1];

                // reservation:122|1df846dd-20ec-4a22-9358-09080d5b5031
                String key = expiredKey.split("\\|")[0];
                String productId = key.split(":")[1];

                ReservationExpiredEvent event = new ReservationExpiredEvent(
                                productId,
                                reservation.reserved());

                // 4. Save OUTBOX EVENT
                outboxEventPublisher.publish(productId, EventType.RESERVATION_EXPIRED.name(), event);

                historicalPurchaseRepository.update(

                                new PurchaseHistory(
                                                reservationId,
                                                Status.EXPIRED.name()));

                LOG.info("Reservation processed successfully");

        }

}