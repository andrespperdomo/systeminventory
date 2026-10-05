/*package com.inventory.infrastructure.redis;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class RedisTestResource implements QuarkusTestResourceLifecycleManager {

    private GenericContainer<?> redis;

    @Override
    public Map<String, String> start() {

        redis = new GenericContainer<>(DockerImageName.parse("redis:7.2"))
                .withExposedPorts(6379);

        redis.start();

        return Map.of(
                "quarkus.redis.hosts",
                "redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));
    }

    @Override
    public void stop() {

        if (redis != null) {
            redis.stop();
        }
    }
}*/