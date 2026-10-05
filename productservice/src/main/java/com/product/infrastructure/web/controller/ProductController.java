package com.product.infrastructure.web.controller;

import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import com.product.application.command.CreateProductCommand;
import com.product.application.command.ListProductCommand;
import com.product.application.usecase.CreateProductUseCase;
import com.product.application.usecase.GetProductUseCase;
import com.product.application.usecase.ListProductUseCase;
import com.product.domain.model.Product;
import com.product.infrastructure.web.mapper.ListProductMapper;
import com.product.infrastructure.web.request.ListProductRequest;
//import com.product.dto.PaginatorDTO;
//import com.product.dto.ProductDTO;
//import com.product.infrastructure.web.mapper.ListProductMapper;
import com.product.infrastructure.web.mapper.ProductMapper;
import com.product.infrastructure.web.request.CreateProductRequest;
import com.product.infrastructure.web.response.ProductListResponse;
//import com.product.infrastructure.web.request.ListProductRequest;
//import com.product.infrastructure.web.response.ProductListResponse;
import com.product.infrastructure.web.response.ProductResponse;
import com.product.shared.utils.PageResult;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/product")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductController {

    @Inject
    CreateProductUseCase createProductUseCase;

    @Inject
    GetProductUseCase getProductUseCase;

    @Inject
    ListProductUseCase listProductUseCase;

    @Inject
    ProductMapper mapper;

    @Inject
    ListProductMapper listProductMapper;

    Logger LOG = Logger.getLogger(ProductController.class.getName());

    @POST
    @RolesAllowed({"USER", "PRODUCT_MANAGER", "ADMIN","ADMINS"})
    @SecurityRequirement(name = "apiKeyAuth")
    @Operation(summary = "Create product", description = "Creates a new product in the system")
    @APIResponse(responseCode = "201", description = "Product created", content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid input data")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response create(@Valid CreateProductRequest request) {
        CreateProductCommand command = mapper.toCommand(request);

        Product product = createProductUseCase.execute(command);
        ProductResponse response = mapper.toResponse(product);

        return Response
                .status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get product by ID", description = "Returns a product based on its ID")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Product found", content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @APIResponse(responseCode = "404", description = "Product not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getById(
            @Parameter(description = "Product ID", required = true) @PathParam("id") String id) {
        Optional<Product> productOpt = getProductUseCase.execute(id);

        if (productOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        ProductResponse response = mapper.toResponse(productOpt.get());

        return Response.ok(response).build(); // 200 OK
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "List products", description = "Returns a paginated list of products")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Successful response", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductListResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid request"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getListProduct(ListProductRequest request) {
        LOG.info("getListProduct");
        ListProductCommand command = listProductMapper.toCommand(request);
        PageResult<Product> products = listProductUseCase.execute(command);
        ProductListResponse response = listProductMapper.toResponse(products);
        return Response.ok(response).build();
    }

}
