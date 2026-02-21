package com.hotel.reservations.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class CacheConfig {

    @Inject
    RedisDataSource redisDataSource;

    @Inject
    ObjectMapper objectMapper;

    public void put(String key, String value, long ttlSeconds) {
        redisDataSource.value(String.class).setex(key, ttlSeconds, value);
    }

    public Optional<String> get(String key) {
        String value = redisDataSource.value(String.class).get(key);
        return Optional.ofNullable(value);
    }

    public void delete(String key) {
        redisDataSource.key().del(key);
    }

    public boolean exists(String key) {
        return redisDataSource.key().exists(key);
    }

    public void putObject(String key, Object value, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(value);
            put(key, json, ttlSeconds);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    public <T> Optional<T> getObject(String key, Class<T> type) {
        return get(key).map(json -> {
            try {
                return objectMapper.readValue(json, type);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize JSON to object", e);
            }
        });
    }
}
