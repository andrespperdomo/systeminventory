package com.inventory.infrastructure.persistence;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class OutboxEventEntity {

    @Id
    @GeneratedValue
    public Long id;

    public String aggregateId;
    public String type;
    public String payload;

    public String status; // PENDING, SENT
    public int retries;
    public LocalDate createDate = LocalDate.now();
}