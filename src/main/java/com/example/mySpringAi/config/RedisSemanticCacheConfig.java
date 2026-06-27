package com.example.mySpringAi.config;

import org.springframework.ai.chat.cache.semantic.SemanticCache;
import org.springframework.ai.chat.cache.semantic.SemanticCacheAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.cache.semantic.DefaultSemanticCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.RedisClient;

@Configuration
public class RedisSemanticCacheConfig {

    /**
     * 建立 Jedis RedisClient，連到 spring.data.redis.* 指定的 Redis；後續可提供給 Redis semantic cache 使用
     * Redis: 跑在 Docker 裡，負責存資料
     * Jedis: 跑在你的 Spring Boot app 裡，負責連 Redis
     */
    @Bean
    RedisClient redisClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        return RedisClient.builder().hostAndPort(host, port).build();
    }

    /**
     * 建立 Redis-backed SemanticCache。
     * 它負責把 query embedding 與 ChatResponse 存進 Redis，並用向量相似度查找可重用的回答。
     */
    @Bean
    public SemanticCache semanticCache(RedisClient redisClient, EmbeddingModel embeddingModel) {
        return DefaultSemanticCache.builder()
                .jedisClient(redisClient)
                .embeddingModel(embeddingModel)
                .similarityThreshold(0.9)
                .indexName("mySemantic-cache")
                .prefix("cache:")
                .build();
    }

    /**
     * 建立 SemanticCacheAdvisor。
     * 注意：只有把這個 advisor 加到 ChatClient 的 defaultAdvisors / advisors 後，semantic cache 才會實際生效。
     */
    @Bean
    public SemanticCacheAdvisor semanticCacheAdvisor(SemanticCache semanticCache) {
        return SemanticCacheAdvisor.builder().cache(semanticCache).build();
    }

}
