package com.inventory.infrastructure.rabbitmq.producer;

import java.util.List;

import org.jboss.logging.Logger;

import com.inventory.domain.outbox.OutboxEvent;
import com.inventory.domain.repository.OutboxRepository;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OutboxProcessor {

    private static final Logger LOG =
            Logger.getLogger(OutboxProcessor.class);

    @Inject
    OutboxRepository repository;

    @Inject
    RabbitMQPublisher rabbitMQPublisher;

    @Scheduled(
        every = "20s",
        concurrentExecution = SKIP
    )
    void process() {

        LOG.info("========== OUTBOX PROCESSOR START ==========");

        List<OutboxEvent> events =
                repository.findPending();

        LOG.infof(
            "Found %d pending outbox events",
            events.size()
        );

        for (OutboxEvent event : events) {

            try {

                LOG.infof(
                    "Publishing outbox event id=%s type=%s",
                    event.id(),
                    event.type()
                );

                rabbitMQPublisher
                    .publish(event)
                    .toCompletableFuture()
                    .join();

                repository.markAsSent(event.id());

                LOG.infof(
                    "Outbox event %s marked as SENT",
                    event.id()
                );

            } catch (Exception e) {

                LOG.errorf(
                    e,
                    "Failed to publish outbox event %s",
                    event.id()
                );

                try {

                    repository.incrementRetries(event.id());

                    LOG.warnf(
                        "Outbox event %s retry count incremented",
                        event.id()
                    );

                } catch (Exception retryException) {

                    LOG.errorf(
                        retryException,
                        "Failed to update retries for event %s",
                        event.id()
                    );
                }
            }
        }

        LOG.info("========== OUTBOX PROCESSOR END ==========");
    }
}