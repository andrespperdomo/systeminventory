package com.product.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

import org.jboss.logging.Logger;

import com.product.application.command.CreateProductCommand;
import com.product.application.messaging.EventEnvelope;
import com.product.domain.model.Product;
import com.product.domain.repository.OutboxRepository;
import com.product.domain.repository.ProductRepository;
import com.product.infrastructure.rabbitmq.event.ProductCreatedEvent;
import com.product.infrastructure.rabbitmq.model.OutboxEvent;
import com.product.infrastructure.rabbitmq.enums.Status;
import com.product.shared.utils.JsonUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.product.domain.event.EventType;
import com.product.domain.event.ReservationCreatedEvent;

@ApplicationScoped
public class CreateProductUseCase {

    private final ProductRepository repository;

    private final OutboxRepository outboxRepository;

    private static final Logger LOG = Logger.getLogger(CreateProductUseCase.class);

    @Inject
    public CreateProductUseCase(ProductRepository productRepository, OutboxRepository outboxRepository) {
        this.repository = productRepository;
        this.outboxRepository = outboxRepository;
    }

    public Product execute(CreateProductCommand command) {
        // 1. Build domain object (NO ID from command)
        LOG.infof("Create product | productName=%s price=%s", command.name(), command.price());
        Product product = new Product(
                "",
                command.name(),
                command.description(),
                command.price(),
                command.cost());

        boolean productExists = repository.findByName(command.name());
        if(productExists){
            LOG.infof("Product already exists | productName=%s", command.name());
            throw new RuntimeException("Product already exists");
        }        
                

        // 3. Save product
        Product saved = repository.save(product);
        LOG.infof("Product saved | productId=%s productName=%s", saved.id(), saved.name());

        // 4. Create OUTBOX event
        ProductCreatedEvent ProductEvent = new ProductCreatedEvent();
        ProductEvent.id = saved.id();
        ProductEvent.name = saved.name();
        ProductEvent.price = saved.price();

       

        EventEnvelope<ProductCreatedEvent> envelope = new EventEnvelope<>(EventType.PRODUCT_CREATED.name(),
                ProductEvent);

        outboxRepository.save(new OutboxEvent(
                Long.parseLong(saved.id()),
                saved.id().toString(),
                EventType.PRODUCT_CREATED.name(),
                JsonUtil.toJson(envelope),
                Status.PENDING.name(), 0));

        ReservationCreatedEvent reservationEvent=new ReservationCreatedEvent(
                Long.parseLong(saved.id()),
                UUID.randomUUID().toString(),
                "1",
                command.price().intValue(),
                BigDecimal.valueOf(saved.price()),
                java.time.LocalDateTime.now().toString()
        );   
        
        EventEnvelope<ReservationCreatedEvent> envelopeReservation = new EventEnvelope<>(EventType.RESERVATION_CREATED.name(),
                reservationEvent);

                 outboxRepository.save(new OutboxEvent(
                Long.parseLong(saved.id()),
                saved.id().toString(),
                EventType.RESERVATION_CREATED.name(),
                JsonUtil.toJson(envelopeReservation),
                Status.PENDING.name(), 0));

        LOG.infof("Create product | ReservationCreatedEvent///////////////////////////////// productName=%s price=%s", command.name(), command.price());        

        return saved;
    }

}
