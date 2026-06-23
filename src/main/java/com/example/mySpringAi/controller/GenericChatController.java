package com.example.mySpringAi.controller;

import com.example.mySpringAi.payload.GenericChatPayload;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GenericChatController {

    private final ChatClient openaiJdbcChatClient;
    private final ChatClient ollamaJdbcChatClient;
    private final ChatClient openaiInMemoryChatClient;

    /**
     * 當你引入 Spring AI（例如 spring-ai-openai-spring-boot-starter 或其他 provider），Spring 會自動建立一個 ChatClient.Builder Bean，並把必要的設定（例如 model、API Key）都注入。
     */
    @Autowired
    public GenericChatController(
            @Qualifier("openaiChatClient-jdbcChatMemory") ChatClient openaiJdbcChatClient,
            @Qualifier("ollamaChatClient-jdbcChatMemory") ChatClient ollamaJdbcChatClient,
            @Qualifier("openaiChatClient-inChatMemory") ChatClient openaiInMemoryChatClient
    ) {
        this.openaiJdbcChatClient = openaiJdbcChatClient;
        this.ollamaJdbcChatClient = ollamaJdbcChatClient;
        this.openaiInMemoryChatClient = openaiInMemoryChatClient;
    }

    /**
     * 把使用者訊息交給 OpenAI，並用 userName 當作記憶 key，讓同一個 userName 的後續請求可以延續先前對話。
     * 使用 inMemory 記憶體，只會在當前 session 中保存對話記憶。
     */
    @PostMapping("/openai/chat-inMemory")
    public String openaiChatInMemory(@RequestBody GenericChatPayload genericChatPayload, @RequestHeader("userName") String userName) {

        return openaiInMemoryChatClient.prompt(genericChatPayload.message())
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, userName)) // 將 userName 設為 conversationId，讓 MessageChatMemoryAdvisor 讀寫該使用者的聊天記憶
                .call().content();
    }

    /**
     * 把使用者訊息交給 OpenAI，並用 userName 當作記憶 key，讓同一個 userName 的後續請求可以延續先前對話。
     * 使用 JDBC 記憶體，會在資料庫中保存對話記憶。
     */
    @PostMapping("/openai/chat-jdbc")
    public String openaiChat(@RequestBody GenericChatPayload genericChatPayload, @RequestHeader("userName") String userName) {

        return openaiJdbcChatClient.prompt(genericChatPayload.message())
                // 將 OpenAI 對話的 conversationId 設為 openai-{userName}，讓 MessageChatMemoryAdvisor 只讀寫這組 JDBC 聊天記憶
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, "openai-" + userName)) // 用 openai- 前綴隔離 OpenAI 的聊天記憶，避免和其他模型 provider 共用同一個 conversationId
                .call().content();
    }

    /**
     * 把使用者訊息交給 Ollama，並用 userName 當作記憶 key，讓同一個 userName 的後續請求可以延續先前對話。
     * 使用 JDBC 記憶體，會在資料庫中保存對話記憶。
     */
    @PostMapping("/ollama/chat-jdbc")
    public String ollamaChat(@RequestBody GenericChatPayload genericChatPayload, @RequestHeader("userName") String userName) {
        return ollamaJdbcChatClient.prompt(genericChatPayload.message())
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, "ollama-" + userName)) // 用 ollama- 前綴隔離 Ollama 的聊天記憶，避免和其他模型 provider 共用同一個 conversationId
                .call().content();
    }
}

/**
 * note:
 * MessageChatMemoryAdvisor 只會讀寫相同 conversationId 的聊天記憶。
 */
