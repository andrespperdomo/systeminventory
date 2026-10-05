package com.inventory.infrastructure.rabbitmq.model;

public record OutboxEvent(
        // Long id,
        String aggregateId,
        String type,
        String payload,
        String status,
        int retries) {

    public OutboxEvent markSent() {
        return new OutboxEvent(
                // this.id,
                this.aggregateId,
                this.type,
                this.payload,
                "SENT",
                this.retries);
    }

    public OutboxEvent incrementRetries() {
        return new OutboxEvent(
                // this.id,
                this.aggregateId,
                this.type,
                this.payload,
                "PENDING",
                this.retries + 1);
    }

}
