package com.example.mySpringAi.component.rag;

import com.example.mySpringAi.dto.TavilyDto;
import com.example.mySpringAi.payload.TavilyPayload;
import com.google.common.net.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

// 對外 REST API 連線
@Slf4j
public class TavilyWebSearchDocumentRetriever implements DocumentRetriever {

    private static final String TAVILY_API_KEY = "TAVILY_SEARCH_API_KEY"; // 環境變數{TAVILY_SEARCH_API_KEY}
    private static final String TAVILY_BASE_URL = "https://api.tavily.com/search";
    private static final int DEFAULT_RESULT_LIMIT = 5;
    private final int resultLimit;
    private final RestClient restClient;

    private TavilyWebSearchDocumentRetriever(RestClient.Builder restClientBuilder, int resultLimit) {
        String apiKey = System.getenv(TAVILY_API_KEY); // 從環境變數拿 API Key
        Assert.notNull(restClientBuilder, "clientBuilder cannot be null"); // 「如果 clientBuilder 是 null，立刻丟出例外，並顯示指定的錯誤訊息」, 不要拖到後面才 NPE
        this.restClient = restClientBuilder
                .baseUrl(TAVILY_BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey) // 預設帶 Authorization Header
                .build();
        this.resultLimit = resultLimit;
    }

    /**
     * 實作 DocumentRetriever 介面的核心方法：從 Tavily 網路搜尋 API 取得相關文件
     */
    @Override
    public List<Document> retrieve(Query query) {
        // === 步驟 1：記錄查詢內容，方便除錯 ===
        log.info("TavilyWebSearchDocumentRetriever: query = {}", query.text());

        // === 步驟 2：驗證查詢物件不為 null ===
        // 如果 query 是 null，立刻拋出 IllegalArgumentException
        Assert.notNull(query, "query cannot be null");

        // === 步驟 3：提取查詢文字 ===
        String q = query.text();

        // === 步驟 4：驗證查詢文字不為空字串 ===
        // hasText() 會檢查字串不是 null、不是空字串、不是只有空白
        Assert.hasText(q, "query text cannot be empty");

        // === 步驟 5：呼叫 Tavily API 執行網路搜尋 ===
        // POST https://api.tavily.com/search
        // Body: {"query": "使用者問題", "search_depth": "advanced", "max_results": resultLimit}
        // Header: Authorization: Bearer {TAVILY_API_KEY}
        TavilyDto dto = restClient.post()
                .body(new TavilyPayload(q, "advanced", resultLimit))  // 建立請求 payload
                .retrieve()                                            // 執行 HTTP 請求
                .body(TavilyDto.class);                               // 將 JSON 回應反序列化成 TavilyDto

        // === 步驟 6：檢查回應是否有效 ===
        // 如果 dto 是 null「或」結果為空，就回傳空列表
        if (dto == null || CollectionUtils.isEmpty(dto.results())) {
            return List.of();  // 回傳空的不可變列表
        }

        // === 步驟 7：將 Tavily API 回應轉換成 Spring AI Document 格式 ===
        // 預先分配列表容量以提升效能
        List<Document> docs = new ArrayList<>(dto.results().size());

        // 遍歷每個搜尋結果（Hit）
        for (TavilyDto.Hit hit : dto.results()) {
            // 使用 Builder Pattern 建立 Document
            Document doc = Document.builder()
                    .text(hit.content())              // 文件本文（搜尋結果的摘要內容）
                    .metadata("title", hit.title())   // 中繼資料：網頁標題
                    .metadata("url", hit.url())       // 中繼資料：來源 URL
                    .score(hit.score())               // 相關性分數（Tavily 計算的搜尋相關度）
                    .build();
            docs.add(doc);
        }

        // === 步驟 8：回傳轉換完成的文件列表 ===
        // 這些 Documents 可以被 RAG Advisor 或手動流程注入到 LLM prompt 中
        return docs;
    }

    // Builder Pattern
    public static Builder builder() {
        return new Builder();
    }

    // Builder Pattern
    public static final class Builder {
        private RestClient.Builder withRestClientBuilder;
        private int withResultLimit = DEFAULT_RESULT_LIMIT;


        public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
            this.withRestClientBuilder = restClientBuilder;
            return this;
        }

        public Builder resultLimit(int resultLimit) {
            this.withResultLimit = resultLimit;
            return this;
        }

        public TavilyWebSearchDocumentRetriever build() {
            return new TavilyWebSearchDocumentRetriever(withRestClientBuilder, withResultLimit);
        }
    }
}
