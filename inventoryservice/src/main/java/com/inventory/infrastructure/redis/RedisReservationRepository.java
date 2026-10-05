package com.inventory.infrastructure.redis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.domain.repository.CacheRedisRepository;

import io.quarkus.redis.datasource.RedisDataSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.quarkus.redis.datasource.keys.KeyCommands;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;

@ApplicationScoped
public class RedisReservationRepository implements CacheRedisRepository {

    @Inject
    RedisDataSource redisDataSource;


      // Only used for SCAN
    @Inject
    Redis redis;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public <T> void save(
            String key,
            String field,
            T value,
            Duration ttl) {
 try {

        String json = objectMapper.writeValueAsString(value);

        var hash = redisDataSource.hash(
                String.class,
                String.class,
                String.class);

        hash.hset(key, field, json);

        if (ttl != null) {
            redisDataSource.key().expire(key, ttl);
        }

    } catch (JsonProcessingException e) {
        throw new RuntimeException(
                "Error serializing value for key: " + key, e);
    }

     
    }

    @Override
public List<RedisRecord> findAll() {

    List<RedisRecord> records = new ArrayList<>();

    KeyCommands<String> keys = redisDataSource.key(String.class);

    for (String key : keys.keys("*")) {

        String type = keys.type(key).name();

        Object value = switch (type.toLowerCase()) {

            case "hash" -> redisDataSource
                    .hash(String.class, String.class, String.class)
                    .hgetall(key);

            case "string" -> redisDataSource
                    .value(String.class)
                    .get(key);

            case "set" -> redisDataSource
                    .set(String.class)
                    .smembers(key);

            default -> "Unsupported type: " + type;
        };

        records.add(new RedisRecord(key, type, value));
    }

    return records;
}

    @Override
    public void saveProductIndex(String productId,String key){
           redisDataSource
    .set(String.class)
    .sadd("product-index:" + productId, key);

    }


    @Override
  public <T> Optional<T> find(
        String key,
        String field,
        Class<T> clazz) {

    var hash = redisDataSource.hash(
            String.class,
            String.class,
            String.class);

    String json = hash.hget(key.trim(), field);

    if (json == null) {
        return Optional.empty();
    }

    try {

        return Optional.of(
                objectMapper.readValue(json, clazz));

    } catch (JsonProcessingException e) {

        throw new RuntimeException(
                "Error deserializing key: " + key +
                " field: " + field +
                " value: " + json,
                e);
    }
}
    @Override
    public List<String> findKeysByProduct(Long productId) {

    return new ArrayList<>(
        redisDataSource
            .set(String.class)
            .smembers("product-index:" + productId)
    );
}
@Override
public List<Map<String, Object>> findByProduct(String idProduct, String field) {

    List<String> keys = findKeysByProduct(Long.valueOf(idProduct));
    List<Map<String, Object>> result = new ArrayList<>();

    for (String key : keys) {

        Optional<String> json = find(key, field, String.class);
        System.out.println("################################### JSON "+json.get());

        if (json.isPresent()) {

            try {

                Map<String, Object> map = objectMapper.readValue(
                        json.get(),
                        new TypeReference<Map<String, Object>>() {
                        });

                result.add(map);

            } catch (Exception e) {
                throw new RuntimeException("Error parsing JSON for key: " + key, e);
            }
        }
    }
    System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$4");
    result.stream().forEach(System.out::println);
    return result;
}

@Override
public List<ReservationEntry> getReserved(String idProduct, String field) {

    List<String> keys = findKeysByProduct(Long.valueOf(idProduct));

    return keys.stream()
            .map(key -> {
                Optional<String> json = find(key, field, String.class);

                if (json.isEmpty()) {
                    return null;
                }

                try {

                    Map<String, Object> data = objectMapper.readValue(
                            json.get(),
                            new TypeReference<Map<String, Object>>() {});

                    return new ReservationEntry(key, data);

                } catch (Exception e) {
                    throw new RuntimeException(
                            "Error parsing JSON for key: " + key, e);
                }
            })
            .filter(Objects::nonNull)
            .toList();
}



    @Override
    public void delete(String key, String field) {

        var hash = redisDataSource.hash(
                String.class,
                String.class,
                String.class);

        hash.hdel(key, field);
    }
}
