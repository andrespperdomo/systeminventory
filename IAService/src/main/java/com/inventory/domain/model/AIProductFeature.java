package com.inventory.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AIProductFeature {

    private Long productId;

    private int reservations;
    private int purchases;
    private int expired;

    private int reservedQuantity;
    private int purchasedQuantity;
    private int expiredQuantity;

    private BigDecimal revenue;

    private long totalPurchaseTimeMinutes;
    private int purchaseTimeSamples;

    private LocalDateTime lastPurchase;
    private LocalDateTime lastReservation;

    public AIProductFeature(Long productId) {
        this.productId = productId;
        this.reservations = 0;
        this.purchases = 0;
        this.expired = 0;
        this.reservedQuantity = 0;
        this.purchasedQuantity = 0;
        this.expiredQuantity = 0;
        this.revenue = BigDecimal.ZERO;
        this.totalPurchaseTimeMinutes = 0;
        this.purchaseTimeSamples = 0;
    }

    public void registerReservation(
            int quantity,
            String reservationTime) {
        LocalDateTime reservation= LocalDateTime.parse(reservationTime);
        reservations++;
        reservedQuantity += quantity;
        lastReservation = reservation;
    }

    public void registerPurchase(
            int quantity,
            BigDecimal price,
            LocalDateTime purchaseTime,
            long purchaseTimeMinutes) {

        purchases++;
        purchasedQuantity += quantity;

        revenue = revenue.add(
                price.multiply(BigDecimal.valueOf(quantity))
        );

        totalPurchaseTimeMinutes += purchaseTimeMinutes;
        purchaseTimeSamples++;

        lastPurchase = purchaseTime;
    }

    public void registerExpiration(int quantity) {
        expired++;
        expiredQuantity += quantity;
    }

    public BigDecimal getAverageReservedQuantity() {
        if (reservations == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(reservedQuantity)
                .divide(
                        BigDecimal.valueOf(reservations),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    public BigDecimal getAveragePurchaseTimeMinutes() {
        if (purchaseTimeSamples == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(totalPurchaseTimeMinutes)
                .divide(
                        BigDecimal.valueOf(purchaseTimeSamples),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    public BigDecimal getCancellationRate() {
        if (reservations == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(expired)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(reservations),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    public BigDecimal getConversionRate() {
        if (reservations == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(purchases)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(reservations),
                        2,
                        RoundingMode.HALF_UP
                );
    }
}