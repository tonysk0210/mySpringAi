package com.example.mySpringAi.controller;

import com.example.mySpringAi.payload.MessageChatPayload;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RedisSemanticCachingController {
    private final ChatClient redicChatClient;
    private final ChatClient vectorStoreChatClient;

    public RedisSemanticCachingController(@Qualifier("openaiCCNoMemRedisCache") ChatClient redicChatClient,
                                          @Qualifier("openaiCCNoMemVectorStoreCache") ChatClient vectorStoreChatClient) {
        this.redicChatClient = redicChatClient;
        this.vectorStoreChatClient = vectorStoreChatClient;
    }

    @PostMapping("/redisCaching-chat")
    public String redisChat(@RequestBody MessageChatPayload messageChatPayload) {
        return redicChatClient.prompt().user(messageChatPayload.message()).call().content();
    }

    @PostMapping("/vectorStoreCaching-chat")
    public String vectorStoreChat(@RequestBody MessageChatPayload messageChatPayload) {
        return vectorStoreChatClient.prompt().user(messageChatPayload.message()).call().content();
    }
}
