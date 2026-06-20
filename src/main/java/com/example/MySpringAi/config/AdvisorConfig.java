package com.example.mySpringAi.config;

import com.example.mySpringAi.component.rag.MaskingDocumentPostProcessor;
import com.example.mySpringAi.component.rag.TavilyWebSearchDocumentRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class AdvisorConfig {

    // 「專門負責 PDF RAG」的 Advisor Bean
    @Bean
    @Primary
    public RetrievalAugmentationAdvisor pdfRetrievalAugmentationAdvisor(@Qualifier("pdfVectorStore") VectorStore vectorStore) {
        // 建立一個 RetrievalAugmentationAdvisor，內部用 VectorStore 做檢索
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                                // 指定要用的 collection（pdf-collection）
                                .vectorStore(vectorStore)
                                // 回傳前 3 筆相似片段
                                .topK(3)
                                // 相似度過低則忽略
                                .similarityThreshold(0.5)
                                .build())
                .build();
    }

    @Bean
    @Qualifier("TrvilyRAAdvisor")
    public RetrievalAugmentationAdvisor trvilyRetrievalAugmentationAdvisor() {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(
                        TavilyWebSearchDocumentRetriever.builder()
                                .restClientBuilder(RestClient.builder()) // RestClient.builder() 是「Spring Framework 提供的 API」
                                .resultLimit(5)
                                .build()
                ).build();
    }

    @Bean
    @Qualifier("preAndPostRAAdvisor")
    public RetrievalAugmentationAdvisor transformerRetrievalAugmentationAdvisor(@Qualifier("pdfVectorStore") VectorStore vectorStore,
                                                                                @Qualifier("openAiBuilder") ChatClient.Builder chatClientBuilder) {
        // 先把 Query 翻譯成英文，再用 pdfVectorStore 做檢索
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(TranslationQueryTransformer.builder() // pre-retrieval: Query 翻譯
                        .chatClientBuilder(chatClientBuilder.clone()) // 用 clone 避免汙染全域 Builder
                        .targetLanguage("english") // 將原始查詢翻譯為英文
                        .build()
                )
                .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore) // 指定 PDF collection
                                .topK(3) // 回傳最相似的前三筆
                                .similarityThreshold(0.5) // 相似度門檻低於 0.5 就忽略
                                .build()
                )
                .documentPostProcessors( // post-retrieval: masking email & phone number
                        MaskingDocumentPostProcessor.getInstance()
                )
                .build();
    }
}
