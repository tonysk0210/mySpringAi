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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/api")
public class RagController {

    private final ChatClient chatClient;
    private final ChatClient openaiChatClientWithoutMemory;
    private final VectorStore vectorStore;
    private final VectorStore pdfVectorStore;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;
    private final RetrievalAugmentationAdvisor trvilyRAAdvisor;
    private final RetrievalAugmentationAdvisor preAndPostRAAdvisor;

    @Value("classpath:/promptTemplate/RagPromptTemplate.st")
    Resource ragPromptTemplate;

    @Autowired
    public RagController(@Qualifier("openaiChatClient-jdbcChatMemory") ChatClient chatClient,
                         @Qualifier("openaiChatClient-withoutMemory") ChatClient openaiChatClientWithoutMemory,
                         VectorStore vectorStore,
                         @Qualifier("pdfVectorStore") VectorStore pdfVectorStore,
                         RetrievalAugmentationAdvisor retrievalAugmentationAdvisor,
                         @Qualifier("TrvilyRAAdvisor") RetrievalAugmentationAdvisor trvilyRAAdvisor,
                         @Qualifier("preAndPostRAAdvisor") RetrievalAugmentationAdvisor preAndPostRAAdvisor) {
        // 將 Spring AI 的 ChatClient 與向量資料庫元件注入，供 /rag 使用
        this.chatClient = chatClient;
        this.openaiChatClientWithoutMemory = openaiChatClientWithoutMemory;
        this.vectorStore = vectorStore;
        this.pdfVectorStore = pdfVectorStore;
        this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
        this.trvilyRAAdvisor = trvilyRAAdvisor;
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
     * -> 交給 retrievalAugmentationAdvisor
     * -> advisor 使用 pdfVectorStore 到 Qdrant 的 pdf-collection 做 similarity search
     * -> 取回符合 topK / similarityThreshold 條件的 PDF 文件片段
     * -> 將檢索到的 Document 內容補進 prompt/context
     * -> 用不帶 chat memory 的 OpenAI ChatClient 產生回答
     */
    @PostMapping("/ragPdf")
    public String pdf(@RequestBody GenericChatPayload genericChatPayload) {
        return openaiChatClientWithoutMemory.prompt()
                .advisors(retrievalAugmentationAdvisor) // 帶著 retrievalAugmentationAdvisor
                .user(genericChatPayload.message())
                .call().content();
    }

    @PostMapping("/ragTavily")
    public String Tavily(@RequestBody GenericChatPayload genericChatPayload, @RequestHeader("userName") String userName) {
        return chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, "ragTavily-" + userName))
                .advisors(trvilyRAAdvisor)
                .user(genericChatPayload.message())
                .call().content();
    }

    @PostMapping("/preAndPostRAAdvisor")
    public String preRetrieval(@RequestBody GenericChatPayload genericChatPayload, @RequestHeader("userName") String userName) {
        return chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, "ragPrePostProcessing-" + userName))
                .advisors(preAndPostRAAdvisor)
                .user(genericChatPayload.message() + "\n\n(請根據以上內容，務必使用清楚、易理解且專業的繁體中文回答)")
                .call().content();
    }
}
