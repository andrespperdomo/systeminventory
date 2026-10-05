package com.inventory.infrastructure.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "purchase_history")
@Data
public class PurchaseHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String reservationId;
    public String productId;
    public Integer quantity;
    public String type;
    public String idUser;
    public LocalDateTime createdAt;
}