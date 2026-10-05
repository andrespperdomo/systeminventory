package com.inventory.domain.repository;

import java.time.Duration;
import java.util.Optional;

import com.inventory.infrastructure.redis.RedisRecord;
import com.inventory.infrastructure.redis.ReservationEntry;

import java.util.List;
import java.util.Map;



public interface CacheRedisRepository {

        <T> void save(
                        String key,
                        String field,
                        T value,
                        Duration ttl);

        List<RedisRecord> findAll();
        void saveProductIndex(String productId,String key);                

        <T> Optional<T> find(
                        String key,
                        String field,
                        Class<T> type);

        List<String> findKeysByProduct(Long productId);   

        List<Map<String, Object>> findByProduct(String idProduct, String field);     

       List<ReservationEntry> getReserved(String idProduct, String field);    
                    

        void delete(
                        String key,
                        String field);
}
