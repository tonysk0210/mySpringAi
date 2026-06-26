package com.example.mySpringAi.component.rag;

import com.example.mySpringAi.dto.TavilyResponseDto;
import com.example.mySpringAi.payload.TavilyRequestPayload;
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

/**
 * 自定義的 DocumentRetriever：收到 query -> 去 Tavily 搜尋 -> 把搜尋結果轉成 List<Document> -> 回傳
 */
// 對外 REST API 連線
@Slf4j
public class TavilyWebSearchDocumentRetriever implements DocumentRetriever {

    private static final String TAVILY_API_KEY = "TAVILY_SEARCH_API_KEY";
    private static final String TAVILY_BASE_URL = "https://api.tavily.com/search";
    private static final int DEFAULT_RESULT_LIMIT = 5;
    private final int resultLimit;
    private final RestClient restClient;

    /**
     * 建立一個可以呼叫 Tavily Web Search API 的 RestClient，並設定最多要拿幾筆搜尋結果。
     */
    private TavilyWebSearchDocumentRetriever(RestClient.Builder restClientBuilder, int resultLimit) {
        // 1. 驗證 clientBuilder 不為 null
        Assert.notNull(restClientBuilder, "restClientBuilder 不可為 null"); // 如果 restClientBuilder 是 null，立刻丟出 IllegalArgumentException，避免後面呼叫 baseUrl() 時才發生 NullPointerException

        // 2. 從環境變數拿 API Key
        String apiKey = System.getenv(TAVILY_API_KEY);

        // 3. 驗證 apiKey 不為空
        Assert.hasText(apiKey, "TAVILY_API_KEY 環境變數不可為空");

        // 4. 建立 RestClient 物件；所以之後在 retrieve(Query query) 裡面呼叫：restClient.post()　就會直接對 Tavily API 發送 POST request，而且自動帶上 Authorization header
        // restClient 是這個類別用來發 HTTP request 給 Tavily API 的工具。
        this.restClient = restClientBuilder
                .baseUrl(TAVILY_BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey) // 帶上 Authorization header
                .build();

        // 5. 驗證 resultLimit 大於 0
        if (resultLimit <= 0) {
            throw new IllegalArgumentException("resultLimit 必須大於 0");
        }
        this.resultLimit = resultLimit;
    }

    /**
     * 實作 DocumentRetriever 介面的核心方法：從 Tavily 網路搜尋 API 取得相關文件
     */
    @Override
    public List<Document> retrieve(Query query) {
        // 1. user 下的 query 內容
        log.info("TavilyWebSearchDocumentRetriever: user 下的 query = {}", query.text());
        Assert.notNull(query, "query 不可為 null");

        // 2. 提取 user 查詢文字
        String q = query.text();
        Assert.hasText(q, "query 不可為空");

        // 3. 呼叫 Tavily API 進行網路搜尋
        // POST https://api.tavily.com/search
        // Body = {"query": "使用者問題", "search_depth": "advanced", "max_results": resultLimit}
        // Header = Authorization: Bearer {TAVILY_API_KEY}
        TavilyResponseDto responseDto = restClient.post()
                .body(new TavilyRequestPayload(q, "advanced", resultLimit))  // 建立請求 payload: advanced 用比較深入、相關性較高的搜尋模式。
                .retrieve()                                                                                      // 取得 HTTP response
                .body(TavilyResponseDto.class);                                                        // 將 JSON 回應反序列化成 TavilyDto

        // 4. 檢查回應是否有效：如果 responseDto 是 null「或」結果為空，就回傳空列表
        if (responseDto == null || CollectionUtils.isEmpty(responseDto.results())) {
            return List.of();
        }

        // 5. 將 Tavily API 回應轉換成 Spring AI Document 格式
        List<Document> docs = new ArrayList<>(responseDto.results().size());

        // 6. 遍歷每個搜尋結果（Hit）
        for (TavilyResponseDto.Hit hit : responseDto.results()) {
            // 6.1 逐一處理 Tavily 回傳的每一筆搜尋結果，並建立 Document 物件：Document 是 Spring AI 用來表示「一段可被 RAG 使用的資料」的標準格式。
            Document doc = Document.builder()
                    .text(hit.content())              // 真正給 AI 看的搜尋內容：必要
                    .metadata("title", hit.title())        // 來源網頁標題：非必要，但建議保留，因為可以追來源
                    .metadata("url", hit.url())            // 來源網址：非必要，但建議保留，因為可以追來源
                    .score(hit.score())                    // 相關性分數（Tavily 計算的搜尋相關度）：非必要，但建議保留，因為可以看相關性
                    .build();
            docs.add(doc);
        }

        // 7. 回傳轉換完成的文件列表：這些 Documents 可以被 RAG Advisor 或手動流程注入到 LLM prompt 中
        return docs;
    }

    /**
     * 建立 TavilyWebSearchDocumentRetriever 類別的 Builder 類別，使用 Builder Pattern
     */
    // Builder pattern: 建立並回傳一個 Builder，讓外部用鏈式呼叫設定參數
    public static Builder builder() {
        return new Builder();
    }

    // Builder 類別：暫存建立 TavilyWebSearchDocumentRetriever 所需的參數，最後由 build() 建立實例
    public static final class Builder {
        private RestClient.Builder withRestClientBuilder;
        private int withResultLimit = DEFAULT_RESULT_LIMIT;

        /**
         * 私有建構子，防止外部直接呼叫
         */
        private Builder() {
            //外部就不能直接 new Builder()
        }

        /**
         * 設定 RestClient.Builder
         */
        public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
            this.withRestClientBuilder = restClientBuilder;
            return this;
        }

        /**
         * 設定最多要拿幾筆搜尋結果
         */
        public Builder maxResults(int maxResults) {
            if (maxResults <= 0) {
                throw new IllegalArgumentException("maxResults 必須大於 0");
            }
            this.withResultLimit = maxResults;
            return this;
        }

        /**
         * 建立 TavilyWebSearchDocumentRetriever 物件
         */
        public TavilyWebSearchDocumentRetriever build() {
            return new TavilyWebSearchDocumentRetriever(withRestClientBuilder, withResultLimit);
        }
    }
}
