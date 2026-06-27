package com.example.mySpringAi.controller;

import com.example.mySpringAi.payload.GenericChatPayload;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RedisSemanticCachingController {
    private final ChatClient redicChatClient;
    private final ChatClient vectorStoreChatClient;

    public RedisSemanticCachingController(@Qualifier("openaiChatClient-NoMemoryWithCaching") ChatClient redicChatClient,
                                          @Qualifier("openaiChatClient-WithVectorStoreCaching") ChatClient vectorStoreChatClient) {
        this.redicChatClient = redicChatClient;
        this.vectorStoreChatClient = vectorStoreChatClient;
    }

    @PostMapping("/redisCaching-chat")
    public String redisChat(@RequestBody GenericChatPayload genericChatPayload) {
        return redicChatClient.prompt().user(genericChatPayload.message()).call().content();
    }

    @PostMapping("/vectorStoreCaching-chat")
    public String vectorStoreChat(@RequestBody GenericChatPayload genericChatPayload) {
        return vectorStoreChatClient.prompt().user(genericChatPayload.message()).call().content();
    }
}
