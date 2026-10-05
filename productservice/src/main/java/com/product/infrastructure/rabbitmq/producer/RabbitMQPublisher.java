package com.product.infrastructure.rabbitmq.producer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import com.product.infrastructure.rabbitmq.event.EventPublisher;
import com.product.infrastructure.rabbitmq.model.OutboxEvent;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;
import io.quarkus.runtime.Startup;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


@Startup
@ApplicationScoped
public class RabbitMQPublisher implements EventPublisher {

    @Channel("product-events-out")
    @Inject
    Emitter<String> emitter;

    
    @Channel("reservation-events-out")
    @Inject
    Emitter<String> emitterReservation;

    private static final Logger LOG = Logger.getLogger(RabbitMQPublisher.class);

   /*  @PostConstruct
    void init() {
        LOG.info("RabbitMQPublisher initialized=========================");
    }*/
   @Override
public CompletionStage<Void> publish(OutboxEvent event) {

    CompletionStage<Void> stage = switch (event.type()) {
        case "PRODUCT_CREATED" -> emitter.send(event.payload());
        case "RESERVATION_CREATED" -> emitterReservation.send(event.payload());
        default -> throw new IllegalArgumentException(event.type());
    };


    stage.whenComplete((result, throwable) -> {
    if (throwable != null) {
        LOG.errorf(
            throwable,
            "RABBITMQ_PUBLISH_FAILED eventId=%s type=%s",
            event.id(),
            event.type()
        );
    } else {
        LOG.infof(
            "RABBITMQ_PUBLISH_ACK eventId=%s type=%s",
            event.id(),
            event.type()
        );
    }
});

    return stage;
}

/*     @Override
    public CompletionStage<Void> publishProductCreated(OutboxEvent event) {
        //LOG.infof("publishProductCreated | ==================================" + event.payload());
        Message<String> message = Message.of(event.payload())
                .withAck(() -> {
                    // ✅ Broker confirmed delivery
                    return CompletableFuture.completedFuture(null);
                })
                .withNack(throwable -> {
                    System.err.println("Failed to send event: " + event.id());
                    throwable.printStackTrace();
                    return CompletableFuture.completedFuture(null);
                });

        return emitter.send(message.getPayload());
    }

    @Override
    public CompletionStage<Void> publishReservationCreated(OutboxEvent event) {
        LOG.infof("publishReservationCreated | $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ ENTRO" );
        LOG.infof("publishReservationCreated | =======================================================" + event.payload());
        Message<String> message = Message.of(event.payload())
                .withAck(() -> {
                    // ✅ Broker confirmed delivery
                    return CompletableFuture.completedFuture(null);
                })
                .withNack(throwable -> {
                    System.err.println("Failed to send event: " + event.id());
                    throwable.printStackTrace();
                    return CompletableFuture.completedFuture(null);
                });

        return emitterReservation.send(message.getPayload());
    }*/

}
