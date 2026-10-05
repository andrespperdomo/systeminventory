package com.inventory.infrastructure.rabbitmq.event;

public class ProductCreatedEvent {
    public String id;
    public String name;
    public Double price;
}