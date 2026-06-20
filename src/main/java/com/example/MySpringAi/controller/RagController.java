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
    private final VectorStore vectorStore;
    private final VectorStore pdfVectorStore;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;
    private final RetrievalAugmentationAdvisor trvilyRAAdvisor;
    private final RetrievalAugmentationAdvisor preAndPostRAAdvisor;

    @Value("classpath:/promptTemplate/RagPromptTemplate.st")
    Resource ragPromptTemplate;

    /*@Value("classpath:/promptTemplate/ragPdfPromptTemplate.st")
    Resource ragPdfPromptTemplate;*/

    @Autowired
    public RagController(@Qualifier("openaiChatClient-jdbcChatMemory") ChatClient chatClient,
                         VectorStore vectorStore,
                         @Qualifier("pdfVectorStore") VectorStore pdfVectorStore,
                         RetrievalAugmentationAdvisor retrievalAugmentationAdvisor,
                         @Qualifier("TrvilyRAAdvisor") RetrievalAugmentationAdvisor trvilyRAAdvisor,
                         @Qualifier("preAndPostRAAdvisor") RetrievalAugmentationAdvisor preAndPostRAAdvisor) {
        // 將 Spring AI 的 ChatClient 與向量資料庫元件注入，供 /rag 使用
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.pdfVectorStore = pdfVectorStore;
        this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
        this.trvilyRAAdvisor = trvilyRAAdvisor;
        this.preAndPostRAAdvisor = preAndPostRAAdvisor;
    }

    @PostMapping("/rag")
    public String openaiChat(@RequestBody GenericChatPayload genericChatPayload, @RequestHeader("userName") String userName) {
        // === 1. 準備向量搜尋條件，用用戶輸入去找語意相近的文件 ===
        // 一次「向量搜尋條件」的封裝物件
        SearchRequest searchRequest = SearchRequest.builder()
                .query(genericChatPayload.message()) // 用這一句話去找「語意上最接近」的 Documents
                .topK(5)                              // 只要最相似的 前 5 筆 Document
                .similarityThreshold(.5)           // 搜尋的相似度門檻
                .build();

        // === 2. 從向量資料庫取得知識片段，並拼成可注入提示詞的上下文 ===
        List<Document> listOfSimilarDocuments = vectorStore.similaritySearch(searchRequest); // 從向量資料庫撈出最相關的知識片段
        String similarContext = listOfSimilarDocuments.stream().map(Document::getText).collect(Collectors.joining(",\n")); // 將所有 Document 轉成 String

        // === 3. 帶著對話記憶與檢索結果呼叫大模型 ===
        return chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, "rag-" + userName))
                .system(systemSpec -> systemSpec.text(ragPromptTemplate).param("documents", similarContext)) // 將知識片段加到 System Prompt
                .user(genericChatPayload.message()) // 將用戶的訊息加到 User Prompt
                .call().content();
    }

    @PostMapping("/ragPdf")
    public String pdf(@RequestBody GenericChatPayload genericChatPayload, @RequestHeader("userName") String userName) {
        return chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, "ragPdf-" + userName))
                .advisors(retrievalAugmentationAdvisor)
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
