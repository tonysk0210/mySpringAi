package com.example.mySpringAi.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientBuilderConfig {

    @Bean
    @Qualifier("openAiBuilder")
    public ChatClient.Builder openAiChatClientBuilder(OpenAiChatModel model) {
        return ChatClient.builder(model);
    }

    @Bean
    @Qualifier("ollamaBuilder")
    public ChatClient.Builder ollamaChatClientBuilder(OllamaChatModel model) {
        return ChatClient.builder(model);
    }
}

