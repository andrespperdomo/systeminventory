package com.inventory.infrastructure.web.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CorrelationIdFilter implements ContainerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String correlationId = requestContext.getHeaderString(HEADER);

        if (correlationId == null) {
            correlationId = java.util.UUID.randomUUID().toString();
        }

        org.jboss.logging.MDC.put("correlationId", correlationId);
    }
}
