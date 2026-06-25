package com.example.mySpringAi.config;

import com.example.mySpringAi.component.rag.MaskingDocumentPostProcessor;
import com.example.mySpringAi.component.rag.TavilyWebSearchDocumentRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

/**
 * RetrievalAugmentationAdvisor 把「搜尋、取文件、組上下文、塞進 prompt」包成一個 advisor 動作。
 * <p>
 * 使用者問題
 * ↓
 * 拿使用者問題當 query
 * ↓
 * 用 pdfVectorStore 做 similarity search
 * ↓
 * 取 topK = 3 的相關 Document
 * ↓
 * 過濾 similarityThreshold < 0.5 的結果
 * ↓
 * 把 Document 內容補進 prompt/context
 * ↓
 * 送給 LLM 回答
 */
@Configuration
public class AdvisorConfig {

    /**
     * 建立一個專門給 /ragPdf 用的 PDF RAG advisor。
     */
    // 在 ChatClient 呼叫 LLM 前，先根據使用者問題檢索 PDF 文件，再把檢索結果套進 RagPdfPromptTemplate.st。
    @Bean
    @Primary
    public RetrievalAugmentationAdvisor pdfRetrievalAugmentationAdvisor(@Qualifier("pdfVectorStore") VectorStore vectorStore,
                                                                        @Value("classpath:/promptTemplate/RagPdfPromptTemplate.st") Resource ragPdfPromptTemplate) {
        // 1. 建立一個 RetrievalAugmentationAdvisor，用來執行「檢索文件 -> 組裝 prompt -> 改寫 user message」的 RAG 流程。
        return RetrievalAugmentationAdvisor.builder()
                // 2. 設定文件檢索器；收到使用者問題後，advisor 會透過它去找相關 Document。
                .documentRetriever(
                        // 3. 建立一個專門從 VectorStore 做 similarity search 的 retriever。
                        VectorStoreDocumentRetriever.builder()
                                // 4. 使用 pdfVectorStore 作為檢索來源；該 VectorStore 在 VectorStoreConfig 中綁定到 Qdrant 的 pdf-collection。
                                .vectorStore(vectorStore)
                                // 5. 最多取回 3 筆相似度最高的文件片段；若符合門檻的結果少於 3 筆，就只回傳實際符合的筆數。
                                .topK(3)
                                // 6. 只保留相似度達到 0.5 以上的結果，避免太不相關的 PDF 片段被塞進 prompt。
                                .similarityThreshold(0.5)
                                .build())
                // 7. 設定 query augmenter；它負責把檢索到的 Document 內容和原始問題套進 prompt template。
                .queryAugmenter(
                        ContextualQueryAugmenter.builder()
                                // 8. 讀取 RagPdfPromptTemplate.st，並用它包裝檢索結果；template 需包含 {context} 與 {query}。
                                .promptTemplate(new PromptTemplate(ragPdfPromptTemplate))
                                // 9. 建立 ContextualQueryAugmenter；執行時會把 {context} 換成 Document 文字，把 {query} 換成使用者問題。
                                .build())
                // 10. 讓 RetrievalAugmentationAdvisor 比預設 order = 0 的 SimpleLoggerAdvisor 更早執行，log 才能印出被 RAG 改寫後的完整 UserMessage。
                .order(-100)
                // 11. 建立最終的 advisor Bean，供 /ragPdf 透過 .advisors(retrievalAugmentationAdvisor) 使用。
                .build();
        /*
        流程大概是這樣：

        1. 使用者問：
           what are the working hours?

        2. RetrievalAugmentationAdvisor 執行
           -> 用 pdfVectorStore 查 Qdrant
           -> 找到 3 個 Document
           -> 放進 context

        3. ContextualQueryAugmenter 執行
           -> 讀取 RagPdfPromptTemplate.st
           -> 把 {context} 換成查到的 PDF 內容
           -> 把 {query} 換成使用者問題

        4. 產生新的 UserMessage
           內容長這樣：
           你是一位樂於協助的助理...
           CONTEXT:
           ...
           問題：
           what are the working hours?

           回答：

        5. 這整段 UserMessage 被送給 OpenAI

        6. OpenAI 看到最後的「回答：」
           -> 接續產生答案：
           工作時間為每週一至週五的下午2點至晚上11點。
         */

        /**
         * RagPromptTemplate.st
         * -> 給 /rag 手動 system prompt 用
         * -> 可以用 {documents}
         *
         * RagPdfPromptTemplate.st
         * -> 給 RetrievalAugmentationAdvisor / ContextualQueryAugmenter 用
         * -> 必須用 {context} 和 {query}
         */
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
                                                                                @Qualifier("openaiBuilder") ChatClient.Builder chatClientBuilder) {
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
