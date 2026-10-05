package com.product.infrastructure.web.controller;

import com.product.infrastructure.security.AuthService;
import com.product.infrastructure.security.dto.AuthRequest;
import com.product.infrastructure.web.request.LoginRequest;
import com.product.infrastructure.web.response.LoginResponse;


import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import com.product.infrastructure.security.dto.AuthResponse;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public AuthResponse login(AuthRequest request){

        return authService.login(request);

    }

}