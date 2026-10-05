package com.inventory.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "reservation_tracking",
    indexes = {
        @Index(
            name = "idx_reservation_tracking_reservation_id",
            columnList = "reservation_id"
        ),
        @Index(
            name = "idx_reservation_tracking_product_id",
            columnList = "product_id"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationTrackingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false, unique = true)
    private String reservationId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    public ReservationTrackingEntity(
            String reservationId,
            String productId,
            Integer quantity,
            LocalDateTime reservedAt) {

        this.reservationId = reservationId;
        this.productId = productId;
        this.quantity = quantity;
        this.reservedAt = reservedAt;
    }
}