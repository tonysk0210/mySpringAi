package com.example.mySpringAi.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EmbeddingModelConfig {

    // 一個 Qdrant DB 可以有很多模型，但每個模型要有自己的 Collection。
    @Bean
    @Primary
    public EmbeddingModel primaryEmbeddingModel(OpenAiEmbeddingModel openai) {
        return openai;
    }

    @Bean
    public EmbeddingModel secondaryEmbeddingModel(OllamaEmbeddingModel ollama) {
        return ollama;
    }
}
