package com.inventory.infrastructure.persistence;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Data;

@Entity
@Data
public class OutboxEventEntity {

    @Id
    @GeneratedValue
    public Long id;

    public String aggregateId;
    public String type;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    public String payload;

    public String status; // PENDING, SENT
    public int retries;
    public LocalDate createDate = LocalDate.now();
}