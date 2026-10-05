package com.inventory.infrastructure.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;

@Readiness
@ApplicationScoped
public class RabbitMQHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        // simple check (or real connection check)
        return HealthCheckResponse.up("RabbitMQ");
    }
}