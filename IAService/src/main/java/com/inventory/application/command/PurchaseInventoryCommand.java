package com.inventory.application.command;

public record PurchaseInventoryCommand(
                String idProduct,
                String idUser,
                Integer quantity,
                String status,
                Long amount,
                Integer reserved) {
}
