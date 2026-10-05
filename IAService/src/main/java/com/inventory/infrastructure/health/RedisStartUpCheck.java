package com.inventory.infrastructure.health;

import java.util.Collections;
import org.eclipse.microprofile.health.HealthCheckResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.smallrye.health.api.AsyncHealthCheck;

@ApplicationScoped
public class RedisStartUpCheck implements AsyncHealthCheck {

    @Inject
    RedisAPI redisAPI;

    @Override
    public Uni<HealthCheckResponse> call() {
        return redisAPI.ping(Collections.emptyList())
                .map(response -> HealthCheckResponse.up("Redis"))
                .onFailure()
                .recoverWithItem(HealthCheckResponse.down("Redis"));
    }
}