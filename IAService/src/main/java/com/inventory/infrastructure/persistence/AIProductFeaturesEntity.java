package com.inventory.infrastructure.persistence;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_product_features")
public class AIProductFeaturesEntity extends PanacheEntityBase{

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "reservations", nullable = false)
    private Integer reservations = 0;

    @Column(name = "purchases", nullable = false)
    private Integer purchases = 0;

    @Column(name = "expired", nullable = false)
    private Integer expired = 0;

    @Column(name = "revenue", nullable = false, precision = 15, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "purchased_quantity", nullable = false)
    private Integer purchasedQuantity = 0;

    @Column(name = "expired_quantity", nullable = false)
    private Integer expiredQuantity = 0;

    @Column(name = "average_reserved_quantity", precision = 10, scale = 2)
    private BigDecimal averageReservedQuantity;

    @Column(name = "average_purchase_time_minutes", precision = 10, scale = 2)
    private BigDecimal averagePurchaseTimeMinutes;

    @Column(name = "cancellation_rate", precision = 5, scale = 2)
    private BigDecimal cancellationRate;

    @Column(name = "conversion_rate", precision = 5, scale = 2)
    private BigDecimal conversionRate;

    @Column(name = "last_purchase")
    private LocalDateTime lastPurchase;

    @Column(name = "last_reservation")
    private LocalDateTime lastReservation;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "total_purchase_time_minutes", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPurchaseTimeMinutes = BigDecimal.ZERO;

    @Column(name = "purchase_time_samples", nullable = false)
    private Integer purchaseTimeSamples = 0;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getReservations() {
        return reservations;
    }

    public void setReservations(Integer reservations) {
        this.reservations = reservations;
    }

    public Integer getPurchases() {
        return purchases;
    }

    public void setPurchases(Integer purchases) {
        this.purchases = purchases;
    }

    public Integer getExpired() {
        return expired;
    }

    public void setExpired(Integer expired) {
        this.expired = expired;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getPurchasedQuantity() {
        return purchasedQuantity;
    }

    public void setPurchasedQuantity(Integer purchasedQuantity) {
        this.purchasedQuantity = purchasedQuantity;
    }

    public Integer getExpiredQuantity() {
        return expiredQuantity;
    }

    public void setExpiredQuantity(Integer expiredQuantity) {
        this.expiredQuantity = expiredQuantity;
    }

    public BigDecimal getAverageReservedQuantity() {
        return averageReservedQuantity;
    }

    public void setAverageReservedQuantity(BigDecimal averageReservedQuantity) {
        this.averageReservedQuantity = averageReservedQuantity;
    }

    public BigDecimal getAveragePurchaseTimeMinutes() {
        return averagePurchaseTimeMinutes;
    }

    public void setAveragePurchaseTimeMinutes(BigDecimal averagePurchaseTimeMinutes) {
        this.averagePurchaseTimeMinutes = averagePurchaseTimeMinutes;
    }

    public BigDecimal getCancellationRate() {
        return cancellationRate;
    }

    public void setCancellationRate(BigDecimal cancellationRate) {
        this.cancellationRate = cancellationRate;
    }

    public BigDecimal getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(BigDecimal conversionRate) {
        this.conversionRate = conversionRate;
    }

    public LocalDateTime getLastPurchase() {
        return lastPurchase;
    }

    public void setLastPurchase(LocalDateTime lastPurchase) {
        this.lastPurchase = lastPurchase;
    }

    public LocalDateTime getLastReservation() {
        return lastReservation;
    }

    public void setLastReservation(LocalDateTime lastReservation) {
        this.lastReservation = lastReservation;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BigDecimal getTotalPurchaseTimeMinutes() {
    return totalPurchaseTimeMinutes;
}

public void setTotalPurchaseTimeMinutes(BigDecimal totalPurchaseTimeMinutes) {
    this.totalPurchaseTimeMinutes = totalPurchaseTimeMinutes;
}

public Integer getPurchaseTimeSamples() {
    return purchaseTimeSamples;
}

public void setPurchaseTimeSamples(Integer purchaseTimeSamples) {
    this.purchaseTimeSamples = purchaseTimeSamples;
}
 
}
