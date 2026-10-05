package com.product.infrastructure.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.product.application.messaging.EventEnvelope;
import com.product.domain.event.EventType;
import com.product.infrastructure.rabbitmq.enums.Status;
import com.product.infrastructure.rabbitmq.event.ProductCreatedEvent;
import com.product.infrastructure.rabbitmq.model.OutboxEvent;
import com.product.shared.utils.JsonUtil;

@QuarkusTest
class RabbitMQIntegrationTest {

    private static final String PRODUCT_ID = "20";
    private static final String PRODUCT_NAME = "My product test";
    private static final double PRODUCT_PRICE = 35435436.98;

    @Inject
    TestPublisher publisher;

    @Inject
    TestConsumer consumer;

    @Inject
    ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        consumer.clear();
    }

    @Test
    void shouldSendAndReceiveProductEvent() throws Exception {

        // GIVEN
        OutboxEvent outboxEvent = buildOutboxEvent();

        // WHEN
        publisher.send(outboxEvent.payload())
                .toCompletableFuture()
                .orTimeout(5, TimeUnit.SECONDS)
                .join();

        String receivedMessage = consumer.awaitMessage();

        // THEN
        assertNotNull(
                receivedMessage,
                "Expected message from RabbitMQ"
        );

        EventEnvelope<ProductCreatedEvent> envelope =
                mapper.readValue(
                        receivedMessage,
                        new TypeReference<EventEnvelope<ProductCreatedEvent>>() {
                        }
                );

        assertAll(
                () -> assertEquals(
                        EventType.PRODUCT_CREATED.name(),
                        envelope.eventType()
                ),

                () -> assertNotNull(
                        envelope.data(),
                        "Expected event data"
                ),

                () -> assertEquals(
                        PRODUCT_ID,
                        envelope.data().id
                ),

                () -> assertEquals(
                        PRODUCT_NAME,
                        envelope.data().name
                ),

                () -> assertEquals(
                        PRODUCT_PRICE,
                        envelope.data().price
                )
        );
    }

    // =========================================================
    // BUILDERS
    // =========================================================

    private ProductCreatedEvent buildProductCreatedEvent() {

        ProductCreatedEvent event = new ProductCreatedEvent();

        event.id = PRODUCT_ID;
        event.name = PRODUCT_NAME;
        event.price = PRODUCT_PRICE;

        return event;
    }

    private OutboxEvent buildOutboxEvent() {

        ProductCreatedEvent productEvent =
                buildProductCreatedEvent();

        EventEnvelope<ProductCreatedEvent> envelope =
                new EventEnvelope<>(
                        EventType.PRODUCT_CREATED.name(),
                        productEvent
                );

        return new OutboxEvent(
                null,
                PRODUCT_ID,
                EventType.PRODUCT_CREATED.name(),
                JsonUtil.toJson(envelope),
                Status.PENDING.name(),
                0
        );
    }
}