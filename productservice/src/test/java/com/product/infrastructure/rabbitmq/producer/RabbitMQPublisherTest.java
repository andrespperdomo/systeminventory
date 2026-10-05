package com.product.infrastructure.rabbitmq.producer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.Test;

import com.product.domain.repository.OutboxRepository;
import com.product.infrastructure.rabbitmq.model.OutboxEvent;

class RabbitMQPublisherTest {

    @Test
    void shouldReturnACompletionStageWhenPublishingProductEvent() throws Exception {
        RabbitMQPublisher publisher = new RabbitMQPublisher();

        @SuppressWarnings("unchecked")
        Emitter<String> emitter = mock(Emitter.class);
        @SuppressWarnings("unchecked")
        Emitter<String> reservationEmitter = mock(Emitter.class);

        when(emitter.send(anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(reservationEmitter.send(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        var emitterField = RabbitMQPublisher.class.getDeclaredField("emitter");
        emitterField.setAccessible(true);
        emitterField.set(publisher, emitter);

        var reservationField = RabbitMQPublisher.class.getDeclaredField("emitterReservation");
        reservationField.setAccessible(true);
        reservationField.set(publisher, reservationEmitter);

        OutboxEvent event = new OutboxEvent(1L, "prod-1", "PRODUCT_CREATED", "{\"test\":true}", "PENDING", 0);

        CompletionStage<Void> result = publisher.publish(event);

        assertNotNull(result, "The sender should return a CompletionStage instead of null");
        verify(emitter).send("{\"test\":true}");
    }

    @Test
    void shouldMarkOutboxEventAsSentAfterSuccessfulPublish() throws Exception {
        OutboxProcessor processor = new OutboxProcessor();
        OutboxRepository repository = mock(OutboxRepository.class);
        RabbitMQPublisher publisher = mock(RabbitMQPublisher.class);

        OutboxEvent event = new OutboxEvent(1L, "prod-1", "PRODUCT_CREATED", "{\"test\":true}", "PENDING", 0);
        when(repository.findPending()).thenReturn(List.of(event));
        when(publisher.publish(event)).thenReturn(CompletableFuture.completedFuture(null));

        var repositoryField = OutboxProcessor.class.getDeclaredField("repository");
        repositoryField.setAccessible(true);
        repositoryField.set(processor, repository);

        var publisherField = OutboxProcessor.class.getDeclaredField("rabbitMQPublisher");
        publisherField.setAccessible(true);
        publisherField.set(processor, publisher);

        processor.process();

        verify(repository).save(argThat(saved ->
                saved.id().equals(event.id())
                        && "SENT".equals(saved.status())
                        && saved.retries() == event.retries()));
    }
}
