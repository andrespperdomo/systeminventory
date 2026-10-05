package com.inventory.application.usecase;

import java.time.Duration;

import org.jboss.logging.Logger;

import com.inventory.application.command.PurchaseInventoryCommand;
import com.inventory.application.command.SaveHistoricalPurchaseCommand;
import com.inventory.application.service.OutboxEventPublisher;
import com.inventory.domain.event.EventType;
import com.inventory.domain.exception.InsufficientStockException;
import com.inventory.domain.exception.InventoryNotFoundException;
import com.inventory.domain.model.Inventory;
import com.inventory.domain.model.PurchaseHistory;
import com.inventory.domain.repository.CacheRedisRepository;
import com.inventory.domain.repository.HistoricalPurchaseRepository;
import com.inventory.infrastructure.rabbitmq.event.InventoryConfirmEvent;
import com.inventory.infrastructure.rabbitmq.event.ReservationEvent;
import com.inventory.infrastructure.redis.ReservationRedisRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SaveHistoricalPurchase {

        private static final Logger LOG = Logger.getLogger(UpdateInventoryUseCase.class);

        private final HistoricalPurchaseRepository historicalPurchaseRepository;


        private final OutboxEventPublisher outboxEventPublisher;

        @Inject
        public SaveHistoricalPurchase(HistoricalPurchaseRepository historicalPurchaseRepository,
                        ReservationRedisRepository reservationRedisRepository,
                        CacheRedisRepository cacheRedisRepository,
                        OutboxEventPublisher outboxEventPublisher) {
                this.historicalPurchaseRepository = historicalPurchaseRepository;
                this.outboxEventPublisher = outboxEventPublisher;
        }

        public PurchaseHistory execute(SaveHistoricalPurchaseCommand command) {
                LOG.infof("Inventory updated | productId=%s", command.idProduct());
                // 1. Build domain object (NO ID from command)

         
                LOG.info("===================HERE ITS RUN REDIS");

                PurchaseHistory purchaseHistory =
                 historicalPurchaseRepository.save(
                                new PurchaseHistory(command.idReservation(), command.idProduct(), command.quantity(), command.type(), command.idUser()));

                // (command, updated);
                return purchaseHistory;

        }

}
