package com.inventory.infrastructure.web.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.inventory.application.command.UpdateInventoryCommand;
import com.inventory.application.usecase.GetInventoryUseCase;
import com.inventory.application.usecase.UpdateInventoryUseCase;
import com.inventory.domain.model.Inventory;
import com.inventory.infrastructure.web.mapper.InventoryMapper;
import com.inventory.infrastructure.web.request.UpdateInventoryRequest;
import com.inventory.infrastructure.web.response.InventoryAttributes;
import com.inventory.infrastructure.web.response.InventoryResponse;
import com.inventory.infrastructure.web.response.InventoryResponse.Data;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    UpdateInventoryUseCase updateInventoryUseCase;

    @Mock
    GetInventoryUseCase getInventoryUseCase;

    @Mock
    InventoryMapper mapper;

    @InjectMocks
    InventoryController controller;

    private UpdateInventoryRequest request;
    private UpdateInventoryCommand command;
    private Inventory inventory;
    private InventoryResponse response;

    @BeforeEach
    void setUp() {

        // REQUEST
        request = new UpdateInventoryRequest();
        request.idProduct = "1";
        request.quantity = 10;

        // COMMAND
        command = new UpdateInventoryCommand("1", 10);

        // DOMAIN
        inventory = new Inventory(1L, "1", null, 20, 50, 80);

        // RESPONSE
   InventoryResponse response =
    new InventoryResponse(
        new InventoryResponse.Data(
            "Inventory",
            "1",
            new InventoryAttributes(
                "1",
                0,
                0,
                50
            )
        )
    );

    }

    // =========================
    // UPDATE INVENTORY
    // =========================
    @Test
    void shouldUpdateInventorySuccessfully() {

        when(mapper.toCommand(request)).thenReturn(command);
        when(updateInventoryUseCase.execute(command)).thenReturn(inventory);
        when(mapper.toResponse(inventory)).thenReturn(response);

        Response result = controller.update(request);

        assertEquals(201, result.getStatus());
        assertEquals(response, result.getEntity());

        verify(mapper).toCommand(request);
        verify(updateInventoryUseCase).execute(command);
        verify(mapper).toResponse(inventory);
    }

    // =========================
    // GET BY ID - FOUND
    // =========================
    @Test
    void shouldReturnInventoryWhenFound() {

        when(getInventoryUseCase.execute("1")).thenReturn(Optional.of(inventory));
        when(mapper.toResponse(inventory)).thenReturn(response);

        Response result = controller.getById("1");

        assertEquals(200, result.getStatus());
        assertEquals(response, result.getEntity());

        verify(getInventoryUseCase).execute("1");
        verify(mapper).toResponse(inventory);
    }

    // =========================
    // GET BY ID - NOT FOUND
    // =========================
    @Test
    void shouldReturn404WhenInventoryNotFound() {

        when(getInventoryUseCase.execute("1")).thenReturn(Optional.empty());

        Response result = controller.getById("1");

        assertEquals(404, result.getStatus());

        verify(getInventoryUseCase).execute("1");
        verifyNoInteractions(mapper);
    }
}