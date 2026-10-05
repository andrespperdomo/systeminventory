package com.inventory.shared.utils;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.application.usecase.ConfirmPurchaseUseCase;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

@ApplicationScoped
public class JsonUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

     private static final Logger LOG = Logger.getLogger(ConfirmPurchaseUseCase.class);

    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Error converting object to JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to object", e);
        }
    }

public static Map<String, Integer> getMapFromPayload(String payload) {

    if (payload == null) {
        return null;
    }

    try {
        return mapper.readValue(payload, Map.class);
    } catch (JsonProcessingException e) {
        LOG.error("Failed to parse payload", e);
        return null;
    }
}
}
