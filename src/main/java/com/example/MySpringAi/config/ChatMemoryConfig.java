package com.example.mySpringAi.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class ChatMemoryConfig {

    /*// 1. JdbcChatMemoryRepository 是「存取層：實際存到資料庫」
    @Bean("jdbcChatMemoryRepo")
    public JdbcChatMemoryRepository jdbcChatMemoryRepo(DataSource dataSource) {
        return JdbcChatMemoryRepository.builder()
                .dataSource(dataSource)
                .build();
    }*/

    @Bean("openai-jdbcChatMemory")
    public ChatMemory openaiJdbcChatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepo) {
        // 2. MessageWindowChatMemory 是「邏輯層：處理訊息數量、保留 system message、裁減舊訊息」
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepo) // 建立一個 ChatMemory 物件，但底層的儲存方式（Repository）使用 JDBC 實作。
                .maxMessages(20)  // 每個 conversationId 的聊天記錄最多保留 20 則
                .build();
    }

    @Bean("ollama-jdbcChatMemory")
    public ChatMemory ollamaJdbcChatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepo) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepo)
                .maxMessages(20)
                .build();
    }

    @Bean("generic-inMemoryChatMemory")
    public ChatMemory inMemoryChatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();  // 不傳 chatMemoryRepository → 預設儲存方式 = InMemoryChatMemoryRepository（存在 JVM 記憶體）
    }


}
