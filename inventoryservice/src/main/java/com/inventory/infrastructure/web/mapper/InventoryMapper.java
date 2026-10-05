package com.inventory.infrastructure.web.mapper;

import com.inventory.application.command.ConfirmPurchaseCommand;
import com.inventory.application.command.PurchaseInventoryCommand;
import com.inventory.application.command.SaveHistoricalPurchaseCommand;
import com.inventory.application.command.UpdateInventoryCommand;
import com.inventory.domain.model.Inventory;
import com.inventory.infrastructure.web.request.ConfirmProductRequest;
import com.inventory.infrastructure.web.request.PurchaseRequest;
import com.inventory.infrastructure.web.request.SaveHistoricalPurchaseRequest;
import com.inventory.infrastructure.web.request.UpdateInventoryRequest;
import com.inventory.infrastructure.web.response.InventoryAttributes;
import com.inventory.infrastructure.web.response.InventoryResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.inventory.infrastructure.web.response.HistoricalInventoryAttributes;
import com.inventory.infrastructure.web.response.HistoricalInventoryResponse;

@Mapper(componentModel = "cdi")
public interface InventoryMapper {

    UpdateInventoryCommand toCommand(UpdateInventoryRequest request);

    PurchaseInventoryCommand toCommandPurchase(PurchaseRequest request);

    ConfirmPurchaseCommand toCommandConfirmPurchase(ConfirmProductRequest request);

    SaveHistoricalPurchaseCommand toCommandSaveHistorical(SaveHistoricalPurchaseRequest request);

    InventoryAttributes toInventoryAttributes(Inventory inventory);

    HistoricalInventoryAttributes toHistoricalAttributes(Inventory inventory);

   default InventoryResponse toResponse(Inventory inventory) {
    return ResponseFactory.inventory(
        inventory.idProduct(),
        toInventoryAttributes(inventory)
    );
}

default HistoricalInventoryResponse toHistoricalResponse(Inventory inventory) {
    return ResponseFactory.historical(
        inventory.idProduct(),
        toHistoricalAttributes(inventory)
    );
}
}