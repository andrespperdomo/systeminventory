package com.inventory.infrastructure.web.controller;

import java.util.Optional;


import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import jakarta.annotation.security.RolesAllowed;

import com.inventory.application.command.ConfirmPurchaseCommand;
import com.inventory.application.command.PurchaseInventoryCommand;
import com.inventory.application.command.UpdateInventoryCommand;
import com.inventory.application.command.SaveHistoricalPurchaseCommand;
import com.inventory.application.usecase.ConfirmPurchaseUseCase;
import com.inventory.application.usecase.GetInventoryUseCase;
import com.inventory.application.usecase.PurchaseInventoryUseCase;
import com.inventory.application.usecase.UpdateInventoryUseCase;
import com.inventory.application.usecase.SaveHistoricalPurchase;
import com.inventory.domain.model.Inventory;
import com.inventory.infrastructure.web.mapper.InventoryMapper;
import com.inventory.infrastructure.web.request.ConfirmProductRequest;
import com.inventory.infrastructure.web.request.PurchaseRequest;
import com.inventory.infrastructure.web.request.SaveHistoricalPurchaseRequest;
import com.inventory.infrastructure.web.request.UpdateInventoryRequest;
import org.jboss.logging.Logger;
import com.inventory.infrastructure.web.response.InventoryResponse;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@SecurityRequirement(name = "apiKeyAuth")
@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryController {

    @Inject
    UpdateInventoryUseCase updateInventoryUseCase;

    @Inject
    GetInventoryUseCase getInventoryUseCase;

    @Inject
    PurchaseInventoryUseCase purchaseInventoryUseCase;

    @Inject
    ConfirmPurchaseUseCase confirmPurchaseUseCase;

    @Inject
    InventoryMapper mapper;

    Logger LOG = Logger.getLogger(InventoryController.class.getName());

    @PUT
    @Path("/confirmPurchase")
    @Operation(summary = "Confirm inventory", description = "Confirm stock inventory in the system")
    @APIResponse(responseCode = "201", description = "Inventory confirm", content = @Content(schema = @Schema(implementation = InventoryResponse.class)))
    @APIResponse(responseCode = "404", description = "Id product not found")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response confirm(@Valid ConfirmProductRequest request) {
        LOG.infof("Confirming purchase | confirm request=%s", request);
        LOG.info("?????????????????????????????????????????????????????????");
        ConfirmPurchaseCommand command = mapper.toCommandConfirmPurchase(request);

        Inventory inventory = confirmPurchaseUseCase.execute(command);
        InventoryResponse response = mapper.toResponse(inventory);

        return Response
                .status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    @PUT
    @Operation(summary = "Update inventory", description = "Update inventory in the system")
    @APIResponse(responseCode = "201", description = "Inventory updated", content = @Content(schema = @Schema(implementation = InventoryResponse.class)))
    @APIResponse(responseCode = "404", description = "Id product not found")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response update(@Valid UpdateInventoryRequest request) {
        UpdateInventoryCommand command = mapper.toCommand(request);

        Inventory inventory = updateInventoryUseCase.execute(command);
        InventoryResponse response = mapper.toResponse(inventory);

        return Response
                .status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    @PUT
     @RolesAllowed({"USER", "PRODUCT_MANAGER", "ADMIN","ADMINS"})
@Path("/doingPurchase")
@Operation(summary = "Doing purchase", description = "Doing purchase in the system")
@APIResponse(
    responseCode = "201",
    description = "Inventory updated",
    content = @Content(
        schema = @Schema(implementation = InventoryResponse.class)
    )
)
@APIResponse(responseCode = "404", description = "Id product not found")
@APIResponse(responseCode = "406", description = "Insufficient stock")
@APIResponse(responseCode = "500", description = "Internal server error")
public Response doingPurchase(@Valid PurchaseRequest request) {

    PurchaseInventoryCommand command = mapper.toCommandPurchase(request);

    Inventory inventory = purchaseInventoryUseCase.execute(command);

    InventoryResponse response = mapper.toResponse(inventory);

    return Response.status(Response.Status.CREATED)
            .entity(response)
            .build();
}

    @GET
    @Path("/{id}")
    @Operation(summary = "Get inventory by ID", description = "Returns a product based on its ID")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Product found", content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
            @APIResponse(responseCode = "404", description = "Product not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getById(
            @Parameter(description = "Product ID", required = true) @PathParam("id") String id) {
        Optional<Inventory> productOpt = getInventoryUseCase.execute(id);

        if (productOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        InventoryResponse response = mapper.toResponse(productOpt.get());

        return Response.ok(response).build(); // 200 OK
    }

    @POST
    @Operation(summary = "Create purchase historic", description = "Create purchase historic in the system")
    @APIResponse(responseCode = "201", description = "Inventory updated", content = @Content(schema = @Schema(implementation = InventoryResponse.class)))
    @APIResponse(responseCode = "404", description = "Id product not found")
    @APIResponse(responseCode = "406", description = "Insufficient available product")
    @APIResponse(responseCode = "409", description = "Insufficient resources product")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response createPurchaseHistoric(@Valid UpdateInventoryRequest request) {
        UpdateInventoryCommand command = mapper.toCommand(request);

        Inventory inventory = updateInventoryUseCase.execute(command);
        InventoryResponse response = mapper.toResponse(inventory);

        return Response
                .status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    @POST
    @Path("/savehistoricalInventory")
    @Operation(summary = "Create purchase historic", description = "Create purchase historic in the system")
    @APIResponse(responseCode = "201", description = "Inventory updated", content = @Content(schema = @Schema(implementation = InventoryResponse.class)))
    @APIResponse(responseCode = "404", description = "Id product not found")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response saveHistoricalPurchase(@Valid SaveHistoricalPurchaseRequest request) {
        SaveHistoricalPurchaseCommand command = mapper.toCommandSaveHistorical(request);

       // Inventory inventory = saveHistoricalPurchase.execute(command);
       // InventoryResponse response = mapper.toResponse(inventory);

        return null;
        /*  Response
                .status(Response.Status.CREATED)
                .entity(response)
                .build();*/
    }

}
