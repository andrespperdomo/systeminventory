package com.inventory.infrastructure.rabbitmq.event;

import java.util.concurrent.CompletionStage;

import com.inventory.domain.outbox.OutboxEvent;

public interface EventPublisher {
    public CompletionStage<Void> publishProductCreated(OutboxEvent outboxEvent);
}
