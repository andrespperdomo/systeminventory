package com.inventory.infrastructure.rabbitmq.producer;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import com.inventory.domain.outbox.OutboxEvent;
import com.inventory.infrastructure.rabbitmq.event.EventPublisher;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Startup
@ApplicationScoped
public class RabbitMQPublisher implements EventPublisher {

        private static final Logger LOG = Logger.getLogger(RabbitMQPublisher.class);

        @Inject
        @Channel("product-events-out")
        Emitter<String> productEventsEmitter;

        @Inject
        @Channel("purchase-events-out")
        Emitter<String> purchaseEventsEmitter;

        @Inject
        @Channel("reservation-events-out")
        Emitter<String> emitterReservation;

        @Inject
        @Channel("reservation-expired-events-out")
        Emitter<String> emitterReservationExpired;

        public CompletionStage<Void> publish(OutboxEvent event) {
                LOG.info("RabbitMQPublisher.publish() - Publishing event: " + event.type() + " with payload: "
                                + event.payload());

                return switch (event.type()) {

                        case "RESERVATION_CREATED" ->
                                emitterReservation
                                                .send(event.payload())
                                                .thenApply(message -> null);

                        case "PURCHASE_CREATED" ->
                                purchaseEventsEmitter
                                                .send(event.payload())
                                                .thenApply(message -> null);

                        case "RESERVATION_EXPIRED" ->
                                emitterReservationExpired
                                                .send(event.payload())
                                                .thenApply(message -> null);

                        default ->
                                throw new IllegalArgumentException(
                                                "Unsupported outbox event type: "
                                                                + event.type());
                };
        }
}