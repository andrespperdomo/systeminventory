package com.inventory.application.command;

public record UpdateInventoryCommand(
                String idProduct,
                Integer quantity) {
}
