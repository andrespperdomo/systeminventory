package com.inventory.infrastructure.rabbitmq.producer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import com.inventory.domain.outbox.OutboxEvent;
import com.inventory.infrastructure.rabbitmq.event.EventPublisher;
import org.eclipse.microprofile.reactive.messaging.Message;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RabbitMQPublisher implements EventPublisher {

    @Channel("product-events-out")
    @Inject
    Emitter<String> emitter;

    public CompletionStage<Void> publishProductCreated(OutboxEvent event) {

        Message<String> message = Message.of(event.payload())
                .withAck(() -> {
                    // Broker confirmed delivery
                    return CompletableFuture.completedFuture(null);
                })
                .withNack(throwable -> {
                    System.err.println("Failed to send event: " + event.payload());
                    throwable.printStackTrace();
                    return CompletableFuture.completedFuture(null);
                });

        return emitter.send(message.getPayload());
    }

    

}
