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
 * 手動建立兩個 VectorStore Bean：
 * pdfVectorStore 操作 Qdrant 的 pdf-collection，用於 PDF/RAG 檢索。
 * cachingVectorStore 操作 Qdrant 的 caching-collection，用於 semantic cache。
 */
@Configuration
public class VectorStoreConfig {

    /**
     * 1. QdrantClient：使用 Spring AI Qdrant starter 自動建立的 Qdrant 連線 client。
     * 2. EmbeddingModel：用來把文字轉成向量；你專案裡目前 EmbeddingModelConfig 用 @Primary 指定 OpenAI embedding 為主要模型
     * 3. ObservationRegistry 是 Spring/Micrometer 的觀測入口，讓 QdrantVectorStore 的操作可以被收集成 metrics 和 traces；metrics 通常給 Prometheus 看，traces 則可以透過 OpenTelemetry 送到 Jaeger 看。
     */

    @Bean("pdfVectorStore")
    public VectorStore pdfVectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel, ObservationRegistry observationRegistry) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel).collectionName("pdf-collection") // 這個 VectorStore 專門存取 Qdrant 裡的 pdf-collection
                .initializeSchema(true) // 啟動時如果 collection/schema 不存在，會嘗試建立
                .batchingStrategy(new TokenCountBatchingStrategy())  // 使用 token 數量分批送 embedding；這也是 Spring AI 的預設策略
                // 接上 Spring/Micrometer 觀測機制；add、delete、similaritySearch 會產生 db.vector.client.operation 指標/追蹤資料 (prometheus, grafana, Jaeger / OpenTelemetry)
                .observationRegistry(observationRegistry)
                .build();
    }

    @Bean("cachingVectorStore")
    public VectorStore cachingVectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel, ObservationRegistry observationRegistry) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel).collectionName("caching-collection") // 這個 VectorStore 專門存取 Qdrant 裡的 caching-collection
                .initializeSchema(true)
                .batchingStrategy(new TokenCountBatchingStrategy())
                .observationRegistry(observationRegistry)
                .build();
    }
}
