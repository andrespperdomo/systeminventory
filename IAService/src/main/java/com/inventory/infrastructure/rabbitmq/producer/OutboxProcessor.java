package com.inventory.infrastructure.rabbitmq.producer;

import java.util.List;

import com.inventory.domain.outbox.OutboxEvent;
import com.inventory.domain.repository.OutboxRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OutboxProcessor {

    @Inject
    OutboxRepository repository;

    @Inject
    RabbitMQPublisher rabbitMQPublisher;

    @Scheduled(every = "5s")
    void process() {

        List<OutboxEvent> events = repository.findPending();

        for (OutboxEvent event : events) {
            try {
                rabbitMQPublisher.publishProductCreated(event).toCompletableFuture()
                        .join();

                event.markSent();

            } catch (Exception e) {
                event.incrementRetries();
            }

            repository.save(event);
        }
    }
}