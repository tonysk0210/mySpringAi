package com.example.mySpringAi.config;

import io.qdrant.client.QdrantClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {
    @Bean("pdfVectorStore")
    public VectorStore pdfVectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {

        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName("pdf-collection")
                .initializeSchema(true) //create collection if not exists
                .build();
    }
}
