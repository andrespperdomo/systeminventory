package com.product.infrastructure.rabbitmq.producer;

import java.util.List;

import org.jboss.logging.Logger;

import com.product.domain.repository.OutboxRepository;
import com.product.infrastructure.rabbitmq.model.OutboxEvent;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OutboxProcessor {

    @Inject
    OutboxRepository repository;

    @Inject
    RabbitMQPublisher rabbitMQPublisher;

    private static final Logger LOG = Logger.getLogger(OutboxProcessor.class);

    @Scheduled(every = "15s")
    void process() {

        List<OutboxEvent> events = repository.findPending();

        for (OutboxEvent event : events) {
            try {
                rabbitMQPublisher.publish(event).toCompletableFuture().join();
                OutboxEvent updatedEvent = event.markSent();
                repository.save(updatedEvent);
                LOG.infof("Outbox event %s marked as SENT", event.id());
            } catch (Exception e) {
                OutboxEvent updatedEvent = event.incrementRetries();
                repository.save(updatedEvent);
                LOG.warnf("Outbox event %s failed to publish; retries=%d", event.id(), updatedEvent.retries());
            }
        }
    }
}