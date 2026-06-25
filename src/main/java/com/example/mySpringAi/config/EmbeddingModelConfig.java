package com.example.mySpringAi.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 在專案同時有 OpenAI 和 Ollama embedding model 時，指定「預設注入 EmbeddingModel 介面時要用哪一個」
 * <p>
 * 同一個 Qdrant server 可以有多個 collection；不同 EmbeddingModel 產生的向量通常應放在不同 collection，因為向量維度與語意空間可能不同。
 */
@Configuration
public class EmbeddingModelConfig {

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
