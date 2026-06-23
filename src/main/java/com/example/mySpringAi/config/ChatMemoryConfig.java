package com.example.mySpringAi.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 定義 2 種 ChatMemory 的 Bean，供 ChatClientConfig 使用，分別使用記憶體和資料庫儲存聊天記錄
 */
@Configuration
public class ChatMemoryConfig {

    // InMemoryChatMemoryRepository 是「存取層：實際存到記憶體」
    @Bean("inMemoryChatMemory")
    public ChatMemory inMemoryChatMemory() {

        // 不建 chatMemoryRepository → 預設儲存方式 = InMemoryChatMemoryRepository（存在 JVM 記憶體）
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    //　JdbcChatMemoryRepository 是「存取層：實際存到資料庫」
    @Bean("jdbcChatMemory")
    public ChatMemory jdbcChatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepo) {

        // JdbcChatMemoryRepository 來自 pom.xml 所引入的 spring-ai-starter-model-chat-memory-repository-jdbc 自動注入
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepo)
                .maxMessages(20)  // 每個 conversationId 的聊天記錄最多保留 20 則
                .build();
    }

    /*
     * 可選：手動建立 JDBC chat memory repository。
     * 目前專案已透過 spring-ai-starter-model-chat-memory-repository-jdbc
     * 自動配置 JdbcChatMemoryRepository，因此不需要手動宣告這個 Bean。
     */
    /*// 1. JdbcChatMemoryRepository 是「存取層：實際存到資料庫」
    @Bean("jdbcChatMemoryRepo")
    public JdbcChatMemoryRepository jdbcChatMemoryRepo(DataSource dataSource) {
        return JdbcChatMemoryRepository.builder()
                .dataSource(dataSource)
                .build();
    }*/

}
