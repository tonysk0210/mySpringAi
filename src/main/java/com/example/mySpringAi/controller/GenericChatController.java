package com.example.mySpringAi.controller;

import com.example.mySpringAi.payload.GenericChatPayload;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

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

    //openai api
    @PostMapping("/openai/chat-jdbc")
    public String openaiChat(@RequestBody GenericChatPayload genericChatPayload, @RequestHeader("userName") String userName) {

        // .prompt() 可以接受 1. 純文字字串 (String), 2. Message (UserMessage / SystemMessage / AssistantMessage), 3. Prompt 物件（完整 Prompt, 4. 包含 Tool / Function 調用的 Prompt
        return openaiJdbcChatClient.prompt(genericChatPayload.message())
                //.advisors(...) 這一行程式碼的確會將參數廣播給所有顧問。在這個場景中，雖然 SimpleLoggerAdvisor 忽略了它，但 MessageChatMemoryAdvisor 正是依賴這個參數來完成它的核心職責（即管理記憶體）。Assign userName to CONVERSATION_ID which is used for MessageChatMemoryAdvisor
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, "openai-" + userName))
                .call().content();
    }

    //ollama api
    @PostMapping("/ollama/chat-jdbc")
    public String ollamaChat(@RequestBody GenericChatPayload genericChatPayload, @RequestHeader("userName") String userName) {
        return ollamaJdbcChatClient.prompt(genericChatPayload.message())
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, "ollama-" + userName))
                .call().content();
    }

    /*in memory api*/
    //openai api

    /**
     *
     */
    @PostMapping("/openai/chat-inMemory")
    public String openaiChatInMemory(@RequestBody GenericChatPayload genericChatPayload, @RequestHeader("userName") String userName) {

        return openaiInMemoryChatClient.prompt(genericChatPayload.message())
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, userName))
                .call().content();
    }
}
