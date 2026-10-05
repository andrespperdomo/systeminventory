package com.inventory.domain.model;

import com.inventory.domain.exception.InsufficientStockException;

public record Inventory(
        Long id,
        String idProduct,
        String idUser,
        Integer reserved,
        Integer available,
        Integer quantity) {

    public static Inventory withReserved(String productId, Integer available, Integer reserved) {
        return withReserved(productId, null, available, reserved);
    }

    public static Inventory withReserved(String productId, String idUser, Integer available, Integer reserved) {
        return new Inventory(null, productId, idUser, reserved, available, 0);
    }

    public Inventory {
        quantity = quantity == null ? 0 : quantity;
        available = available == null ? 0 : available;
        reserved = reserved == null ? 0 : reserved;

    }

    public Inventory decrease(int amount) {
        if (this.quantity < amount) {
            throw new InsufficientStockException(this.quantity, amount);
        }
        return new Inventory(id, idProduct, idUser, reserved, available, quantity - amount);
    }

    public Inventory addStock(Integer amount) {
        int safeAmount = amount == null ? 0 : amount;
        int safeQuantity = quantity == null ? 0 : quantity;

        Integer updatedQuantity = safeQuantity + safeAmount;
        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ " + updatedQuantity);
        return new Inventory(
                id,
                idProduct,
                idUser,
                reserved,
                available + safeAmount,
                updatedQuantity);
    }

    public Inventory releaseReservation(Integer reservedToRelease) {

    if (reservedToRelease == null || reservedToRelease <= 0) {
        throw new IllegalArgumentException("Reserved quantity must be greater than zero.");
    }

    if (reserved < reservedToRelease) {
        throw new IllegalStateException(
                String.format(
                        "Cannot release %d reserved items. Current reserved: %d",
                        reservedToRelease,
                        reserved));
    }

    return new Inventory(
            id,
            idProduct,
            idUser,
            reserved - reservedToRelease,
            available + reservedToRelease,
            quantity);
}
}