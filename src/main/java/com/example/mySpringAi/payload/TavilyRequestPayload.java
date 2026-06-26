package com.example.mySpringAi.payload;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

// Java 欄位 searchDepth 轉成 JSON 時，要變成 search_depth；maxResults 要變成 max_results。
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record
TavilyRequestPayload(String query, String searchDepth, int maxResults) {
}

/*
Tavily API 需要的 JSON 是 snake_case：
    {
      "query": "Spring AI 是什麼",
      "search_depth": "advanced",
      "max_results": 5
    }
*/
