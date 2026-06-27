package com.example.mySpringAi.controller;

import com.example.mySpringAi.payload.GenericChatPayload;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RedisSemanticCachingController {
    private final ChatClient chatClient;

    public RedisSemanticCachingController(@Qualifier("openaiChatClient-NoMemoryWithCaching") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/caching-chat")
    public String chat(@RequestBody GenericChatPayload genericChatPayload) {
        return chatClient
                .prompt()
                .user(genericChatPayload.message())
                .call().content();
    }
}
