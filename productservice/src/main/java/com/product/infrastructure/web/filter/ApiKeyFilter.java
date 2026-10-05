package com.product.infrastructure.web.filter;

import java.io.IOException;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;



@Provider
@ApplicationScoped
public class ApiKeyFilter implements ContainerRequestFilter {

    @ConfigProperty(name = "app.api-key")
    String expectedKey;
@Override
public void filter(ContainerRequestContext requestContext) {
    System.out.println("PATH = " + requestContext.getUriInfo().getPath());

    String path = requestContext.getUriInfo().getPath();

    if (path.startsWith("q")
            || path.startsWith("swagger")
            || path.startsWith("openapi")) {
        System.out.println("Skipping authentication");
        return;
    }

    System.out.println("Authenticating...");
}

    
}






  /*   @Inject
    @ConfigProperty(name = "app.api-key")
    String apiKey;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
         System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% PATH"+path);

        if (path.startsWith("q")) {
            return;
        }

         if (path.startsWith("/q") || path.startsWith("/swagger") || path.startsWith("/openapi")) {
            return;
        }

        String header = requestContext.getHeaderString("x-api-key");
        if (header == null || !header.equals(apiKey)) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Missing or invalid x-api-key header")
                    .build());
        }
    }
}*/
