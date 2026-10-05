package com.inventory.infrastructure.rabbitmq.consumer;

import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.inventory.application.command.InventoryProductCommand;
import com.inventory.application.messaging.EventEnvelope;
import com.inventory.domain.repository.InventoryRepository;
import com.inventory.infrastructure.rabbitmq.event.ProductCreatedEvent;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import com.inventory.application.usecase.HandleProductCreatedUseCase;
import com.inventory.application.usecase.InventoryProductUseCase;

@ApplicationScoped
public class RabbitMQConsumer {

    private static final Logger LOG = Logger.getLogger(RabbitMQConsumer.class);

    public static BlockingQueue<EventEnvelope<ProductCreatedEvent>> messages = new LinkedBlockingQueue<>();

    ObjectMapper mapper = new ObjectMapper();

    @Inject
    HandleProductCreatedUseCase handleProductCreatedUseCase;

    @Inject
    InventoryProductUseCase inventoryProductUseCase;

    @Incoming("product-events-in")
    @Blocking
    public CompletionStage<Void> receive(Message<String> msg) {
         LOG.info( msg.getPayload()+"&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
        try {
            EventEnvelope<ProductCreatedEvent> event = mapper.readValue(
                    msg.getPayload(),
                    new TypeReference<EventEnvelope<ProductCreatedEvent>>() {
                    });

            // ✅ validate event
            if (!"PRODUCT_CREATED".equals(event.eventType())) {
                return msg.nack(new IllegalArgumentException("Unexpected event type"));
            }

            // ✅ process
            messages.add(event);

            handleProductCreatedUseCase.execute(event.data());

            ProductCreatedEvent productEvent =event.data();
            InventoryProductCommand inventoryProductCommand=
            new InventoryProductCommand(productEvent.id, 
                                      BigDecimal.valueOf(productEvent.price));
            inventoryProductUseCase.execute(inventoryProductCommand);

            return msg.ack();

        } catch (Exception e) {
            LOG.error("Failed to process message: " + msg.getPayload(), e);
            return msg.nack(e);
        }
    }
}
