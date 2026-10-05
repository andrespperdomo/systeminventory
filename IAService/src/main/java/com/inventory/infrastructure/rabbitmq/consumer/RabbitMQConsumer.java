package com.inventory.infrastructure.rabbitmq.consumer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.application.messaging.EventEnvelope;
import com.inventory.application.usecase.PurchaseInventoryUseCase;
import com.inventory.application.usecase.ReservationCreatedUseCase;
import com.inventory.application.usecase.ReservationExpiredUseCase;
import com.inventory.infrastructure.rabbitmq.event.InventoryConfirmEvent;
import com.inventory.infrastructure.rabbitmq.event.InventoryUpdatedEvent;
import com.inventory.infrastructure.rabbitmq.event.ReservationEvent;
import com.inventory.infrastructure.rabbitmq.event.ReservationExpiredEvent;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


@ApplicationScoped
public class RabbitMQConsumer {

    private static final Logger LOG = Logger.getLogger(RabbitMQConsumer.class);

    public static BlockingQueue<EventEnvelope<ReservationEvent>> messages = new LinkedBlockingQueue<>();

    public static BlockingQueue<EventEnvelope<InventoryConfirmEvent>> messagesInventoryUpdate = new LinkedBlockingQueue<>();

    public static BlockingQueue<EventEnvelope<ReservationExpiredEvent>> messagesReservationExpired = new LinkedBlockingQueue<>();

    ObjectMapper mapper = new ObjectMapper();

    @Inject
    ReservationCreatedUseCase reservationCreatedUseCase;

    @Inject
    PurchaseInventoryUseCase purchaseInventoryUseCase;

    @Inject
    ReservationExpiredUseCase  reservationExpiredUseCase;


    @Incoming("reservationcreated-events-in")
    @Blocking
    public CompletionStage<Void> receive(Message<String> msg) {
      
        try {
              LOG.info("############################## escucha receive===========================" + msg.getPayload());
           JavaType type = mapper.getTypeFactory()
    .constructParametricType(
        EventEnvelope.class,
        ReservationEvent.class);

EventEnvelope<ReservationEvent> event =
    mapper.readValue(msg.getPayload(), type);

            // ✅ validate event
            if (!"RESERVATION_CREATED".equals(event.eventType())) {
                return msg.nack(new IllegalArgumentException("Unexpected event type"));
            }

            // ✅ process
            messages.add(event);

            reservationCreatedUseCase.execute(event.data());

            return msg.ack();

        } catch (Exception e) {
            LOG.error("Failed to process message: " + msg.getPayload(), e);
            return msg.nack(e);
        }
    }


    
    @Incoming("purchasecreated-events-in")
    @Blocking
    public CompletionStage<Void> receivePurchase(Message<String> msg) {
      
        try {
              LOG.info("############################## escucha receive purchase===========================" + msg.getPayload());
           JavaType type = mapper.getTypeFactory()
    .constructParametricType(
        EventEnvelope.class,
        InventoryConfirmEvent.class);

EventEnvelope<InventoryConfirmEvent> event =
    mapper.readValue(msg.getPayload(), type);

            // ✅ validate event
            if (!"PURCHASE_CREATED".equals(event.eventType())) {
                return msg.nack(new IllegalArgumentException("Unexpected event type"));
            }

            // ✅ process
            messagesInventoryUpdate.add(event);

            purchaseInventoryUseCase.execute(event.data());

            return msg.ack();

        } catch (Exception e) {
            LOG.error("Failed to process message: " + msg.getPayload(), e);
            return msg.nack(e);
        }
    }

        @Incoming("reservationexpired-events-in")
    @Blocking
    public CompletionStage<Void> reservationExpiredEvent(Message<String> msg) {
      
        try {
              LOG.info("############################## escucha receive expired===========================" + msg.getPayload());
           JavaType type = mapper.getTypeFactory()
    .constructParametricType(
        EventEnvelope.class,
        ReservationExpiredEvent.class);

EventEnvelope<ReservationExpiredEvent> event =
    mapper.readValue(msg.getPayload(), type);

            // ✅ validate event
            if (!"RESERVATION_EXPIRED".equals(event.eventType())) {
                return msg.nack(new IllegalArgumentException("Unexpected event type"));
            }

            // ✅ process
            messagesReservationExpired.add(event);

            reservationExpiredUseCase.execute(event.data());

            return msg.ack();

        } catch (Exception e) {
            LOG.error("Failed to process message: " + msg.getPayload(), e);
            return msg.nack(e);
        }
    }
}
