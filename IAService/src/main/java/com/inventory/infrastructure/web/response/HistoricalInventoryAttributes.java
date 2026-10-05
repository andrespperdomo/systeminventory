package com.inventory.infrastructure.web.response;

public record HistoricalInventoryAttributes (
         String idProduct,
         Integer quantity,
         String status,
         Integer idUser
){ }
