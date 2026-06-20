package com.example.mySpringAi.payload;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) // 告訴 Jackson：這個 class 在 JSON 序列化 / 反序列化時，欄位名稱要用 snake_case
public record TavilyPayload(String query, String searchDepth, int maxResults) {
}
