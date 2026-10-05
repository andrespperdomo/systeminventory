package com.product.infrastructure.persistence;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class OutboxEventEntity {

    @Id
    @GeneratedValue
    public Long id;

    public String aggregateId;
    public String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public String payload;

    public String status; // PENDING, SENT
    public int retries;
    public LocalDate createDate = LocalDate.now();
}