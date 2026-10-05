package com.inventory.infrastructure.apikey;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class ApiKeyFilter implements ContainerRequestFilter {

    @ConfigProperty(name = "inventory.api.key")
    String expectedKey;

    @Override
    public void filter(ContainerRequestContext requestContext) {

        String key = requestContext.getHeaderString("X-API-KEY");

        System.out.println("HEADER:================================================ " + key);
        System.out.println("EXPECTED:============================================== " + expectedKey);

        if (key == null || !key.equals(expectedKey)) {
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }
}