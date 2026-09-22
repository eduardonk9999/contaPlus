package com.contaplus.api.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Products: 5 minutes (frequently updated)
        cacheConfigurations.put(CacheNames.PRODUCTS, defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Categories: 30 minutes (rarely updated)
        cacheConfigurations.put(CacheNames.CATEGORIES, defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Suppliers: 30 minutes (rarely updated)
        cacheConfigurations.put(CacheNames.SUPPLIERS, defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Reports: 2 minutes (aggregated data)
        cacheConfigurations.put(CacheNames.REPORTS, defaultConfig.entryTtl(Duration.ofMinutes(2)));

        // Dashboard: 1 minute (real-time feel)
        cacheConfigurations.put(CacheNames.DASHBOARD, defaultConfig.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
