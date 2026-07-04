package com.example.mySpringAi.config;

import io.micrometer.observation.ObservationRegistry;
import io.qdrant.client.QdrantClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 手動建立一個名叫 pdfVectorStore 的 VectorStore Bean，底層使用 Qdrant，並指定它操作的 collection 是 pdf-collection。
 */
@Configuration
public class VectorStoreConfig {

    /**
     * QdrantClient qdrantClient：使用 Spring AI Qdrant starter 自動建立的 Qdrant 連線 client。
     * EmbeddingModel embeddingModel：用來把文字轉成向量；你專案裡目前 EmbeddingModelConfig 用 @Primary 指定 OpenAI embedding 為主要模型
     */
    @Bean("pdfVectorStore")
    public VectorStore pdfVectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel, ObservationRegistry observationRegistry) {

        return QdrantVectorStore.builder(qdrantClient, embeddingModel).collectionName("pdf-collection") // 這個 VectorStore 專門存取 Qdrant 裡的 pdf-collection
                .initializeSchema(true) // 啟動時如果 collection/schema 不存在，會嘗試建立
                .batchingStrategy(new TokenCountBatchingStrategy())  // 使用 token 數量分批送 embedding；這也是 Spring AI 的預設策略
                .observationRegistry(observationRegistry) // 注入 ObservationRegistry，讓 Micrometer 可以追蹤 db.vector.client.operation 指標
                .build();
    }

    @Bean("cachingVectorStore")
    public VectorStore cachingVectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel, ObservationRegistry observationRegistry) {

        return QdrantVectorStore.builder(qdrantClient, embeddingModel).collectionName("caching-collection") // 這個 VectorStore 專門存取 Qdrant 裡的 pdf-collection
                .initializeSchema(true) // 啟動時如果 collection/schema 不存在，會嘗試建立
                .batchingStrategy(new TokenCountBatchingStrategy())  // 使用 token 數量分批送 embedding；這也是 Spring AI 的預設策略
                .observationRegistry(observationRegistry) // 注入 ObservationRegistry，讓 Micrometer 可以追蹤 db.vector.client.operation 指標
                .build();
    }
}
