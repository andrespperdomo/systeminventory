package com.inventory.infrastructure.web.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.inventory.domain.model.AIProductFeature;
import com.inventory.domain.repository.FeatureRepository;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.inventory.infrastructure.web.request.ExpirationRequest;
import com.inventory.infrastructure.web.request.PurchaseRequest;
import com.inventory.infrastructure.web.request.ReservationRequest;

@Path("/ai/features")
@Tag(name = "AI Product Features")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class AIProductFeatureController {

    @Inject
    FeatureRepository repository;

    @GET
    @Path("/{productId}")
    @Operation(summary = "Get AI feature by product id")
    public Response findByProductId(@PathParam("productId") Long productId) {

        return repository.findByProductId(productId)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

   @POST
    @Path("/{productId}/reservation")
    @Operation(summary = "Register reservation")
    public Response registerReservation(
            @PathParam("productId") Long productId,
            ReservationRequest request) {

        AIProductFeature feature = repository.findByProductId(productId)
                .orElse(new AIProductFeature(
                        productId));

        feature.registerReservation(request.quantity(), LocalDateTime.now().toString());

        repository.save(feature);

        return Response.ok(feature).build();
    }

    @PUT
    @Path("/{productId}/purchase")
    @Operation(summary = "Register purchase")
    public Response registerPurchase(
            @PathParam("productId") Long productId,
            PurchaseRequest request) {

        AIProductFeature feature = repository.findByProductId(productId)
                .orElseThrow();

        feature.registerPurchase(request.quantity(), request.price(), LocalDateTime.now(), 23L);
   
        repository.save(feature);

        return Response.ok(feature).build();
    }

    @PUT
    @Path("/{productId}/expiration")
    @Operation(summary = "Register expiration")
    public Response registerExpiration(
            @PathParam("productId") Long productId,
            ExpirationRequest request) {

        AIProductFeature feature = repository.findByProductId(productId)
                .orElseThrow();

        feature.registerExpiration(request.quantity());

        repository.save(feature);

        return Response.ok(feature).build();
    }
}