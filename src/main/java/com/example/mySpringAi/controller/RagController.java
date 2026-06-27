package com.example.mySpringAi.controller;

import com.example.mySpringAi.payload.GenericChatPayload;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class RagController {

    private final ChatClient openaiChatClientWithoutMemory;
    private final VectorStore vectorStore;
    private final VectorStore pdfVectorStore;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;
    private final RetrievalAugmentationAdvisor tavilyRaAdvisor;
    private final RetrievalAugmentationAdvisor preAndPostRAAdvisor;

    @Value("classpath:/promptTemplate/RagPromptTemplate.st")
    Resource ragPromptTemplate;

    @Autowired
    public RagController(@Qualifier("openaiChatClient-withoutMemory") ChatClient openaiChatClientWithoutMemory,
                         VectorStore vectorStore,
                         @Qualifier("pdfVectorStore") VectorStore pdfVectorStore,
                         RetrievalAugmentationAdvisor retrievalAugmentationAdvisor,
                         @Qualifier("tavilyRaAdvisor") RetrievalAugmentationAdvisor tavilyRaAdvisor,
                         @Qualifier("preAndPostRAAdvisor") RetrievalAugmentationAdvisor preAndPostRAAdvisor) {
        // 將 Spring AI 的 ChatClient 與向量資料庫元件注入，供 /rag 使用
        this.openaiChatClientWithoutMemory = openaiChatClientWithoutMemory;
        this.vectorStore = vectorStore;
        this.pdfVectorStore = pdfVectorStore;
        this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
        this.tavilyRaAdvisor = tavilyRaAdvisor;
        this.preAndPostRAAdvisor = preAndPostRAAdvisor;
    }

    /**
     * 使用者問題
     * -> 去 Qdrant 找相關文件
     * -> 把文件放進 system prompt
     * -> 把使用者問題放進 user prompt
     * -> 用 OpenAI 回答
     */
    @PostMapping("/rag")
    public String openaiChat(@RequestBody GenericChatPayload genericChatPayload) {

        // 1. 建立「搜尋條件」準備向量搜尋條件，用用戶輸入去找語意相近的文件
        SearchRequest searchRequest = SearchRequest.builder()
                .query(genericChatPayload.message())    // 使用者輸入當查詢文字
                .topK(5)                                 // 最多取前 5 筆最相似的 Document
                .similarityThreshold(.5)             // 搜尋的相似度門檻
                .build();

        // 2. 從向量資料庫取得知識片段
        List<Document> listOfSimilarDocuments = vectorStore.similaritySearch(searchRequest); // 從向量資料庫撈出最相關的知識片段

        // 3. 取出每個 Document 的文字內容，串成可放進 prompt 的上下文
        String similarContext = listOfSimilarDocuments.stream().map(Document::getText).collect(Collectors.joining(",\n")); // 將所有 Document 轉成 String

        // 4. 帶著對話記憶與檢索結果呼叫大模型
        return openaiChatClientWithoutMemory.prompt()
                .system(systemSpec -> systemSpec.text(ragPromptTemplate).param("documents", similarContext)) // 讀取 RAG prompt template，並把向量搜尋取得的文件內容填入 {documents}，作為 system prompt 傳給模型
                .user(genericChatPayload.message()) // 將用戶的訊息加到 User Prompt
                .call().content();
    }

    /**
     * 使用 PDF RAG advisor 回答使用者問題。
     * <p>
     * 使用者問題
     * -> ChatClient 在 call() 時執行 retrievalAugmentationAdvisor
     * -> advisor 使用 pdfVectorStore 到 Qdrant 的 pdf-collection 做 similarity search
     * -> 取回符合 topK / similarityThreshold 條件的 PDF Document chunks
     * -> ContextualQueryAugmenter 將 Document 內容套進 RagPdfPromptTemplate.st 的 {context}
     * -> 將使用者問題套進 {query}
     * -> 用不帶 chat memory 的 OpenAI ChatClient 產生回答
     */
    @PostMapping("/ragPdf")
    public String pdf(@RequestBody GenericChatPayload genericChatPayload) {
        return openaiChatClientWithoutMemory.prompt()
                .advisors(retrievalAugmentationAdvisor) // 帶著 retrievalAugmentationAdvisor
                .user(genericChatPayload.message())
                .call().content();
    }

    /**
     * 使用 Tavily RAG advisor 回答使用者問題。
     * <p>
     * 使用者呼叫 /ragTavily
     * -> ChatClient 執行 tavilyRaAdvisor
     * -> RetrievalAugmentationAdvisor 將使用者問題包成 Query
     * -> 呼叫 TavilyWebSearchDocumentRetriever.retrieve(query)
     * -> retrieve() 將搜尋結果轉成 List<Document>
     * -> advisor 把 Document 內容整理進 prompt context
     * -> 使用 openaiChatClientWithoutMemory 呼叫模型產生回答
     */
    @PostMapping("/ragTavily")
    public String tavily(@RequestBody GenericChatPayload genericChatPayload) {
        return openaiChatClientWithoutMemory.prompt()
                .advisors(tavilyRaAdvisor) // 帶著 tavilyRaAdvisor 自定義 retrievalAugmentationAdvisor
                .user(genericChatPayload.message())
                .call().content();
    }

    /**
     * Pre-retrieval:
     * QueryTransformer 把使用者問題翻譯成英文
     * <p>
     * Retrieval:
     * VectorStoreDocumentRetriever 用翻譯後 query 查 pdfVectorStore / Qdrant
     * <p>
     * Post-retrieval:
     * MaskingDocumentPostProcessor 遮罩文件裡的 email / phone
     * <p>
     * Augmentation:
     * ContextualQueryAugmenter 把檢索到的 documents 塞進 RagPdfPromptTemplate.st
     * <p>
     * Generation:
     * OpenAI chat model 產生回答
     */
    @PostMapping("/preAndPostRAAdvisor")
    public String preRetrieval(@RequestBody GenericChatPayload genericChatPayload) {
        return openaiChatClientWithoutMemory.prompt()
                .advisors(preAndPostRAAdvisor)
                .user(genericChatPayload.message())
                .call().content();
    }
}
