package com.example.mySpringAi.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatClientBuilderConfig {

    @Primary
    @Bean("openaiBuilder")
    public ChatClient.Builder openaiBuilder(OpenAiChatModel model) {
        return ChatClient.builder(model);
    }

    @Bean("ollamaBuilder")
    public ChatClient.Builder ollamaBuilder(OllamaChatModel model) {
        return ChatClient.builder(model);
    }
}
