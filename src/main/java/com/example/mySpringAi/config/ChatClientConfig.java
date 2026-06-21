package com.example.mySpringAi.config;

import com.example.mySpringAi.tools.TimeTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAiChatModel 和 OllamaChatModel 是 Spring 自動建立的 Bean 在 pom.xml 引入
 *
 * spring-ai-starter-model-openai 會自動建立 OpenAiChatModel Bean。
 * spring-ai-starter-model-ollama 會自動建立 OllamaChatModel Bean。
 * 因為專案同時有多個 ChatModel 實作，所以這裡直接注入具體型別避免歧義
 */
@Configuration
public class ChatClientConfig {

    @Bean("openaiChatClient-jdbcChatMemory")
    public ChatClient openaiChatClient(OpenAiChatModel openAiChatModel, @Qualifier("openai-jdbcChatMemory") ChatMemory chatMemory) {
        //ChatMemory 已經是一個 Bean（由 MessageWindowChatMemory 建立），並且會被成功注入
        ChatOptions.Builder chatOptions = ChatOptions.builder().temperature(0.5).maxTokens(500);  // 1. 設定模型用的「參數」：
        Advisor jdbcChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build(); // 2. 建立 MessageChatMemoryAdvisor，也就是「會話記憶攔截器」。

        return ChatClient.builder(openAiChatModel)
                .defaultOptions(chatOptions)
                .defaultAdvisors(new SimpleLoggerAdvisor(), jdbcChatMemoryAdvisor) // 3. jdbcChatMemoryAdvisor 加入預設 Advisor（攔截器）
                .defaultSystem("回答時請使用清楚、易理解且專業的繁體中文。")
                .build();
    }

    @Bean("ollamaChatClient-jdbcChatMemory")
    public ChatClient ollmaChatClient(OllamaChatModel ollamaChatModel, @Qualifier("ollama-jdbcChatMemory") ChatMemory chatMemory) {
        ChatOptions.Builder chatOptions = ChatOptions.builder().temperature(0.5).maxTokens(500);
        Advisor jdbcChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return ChatClient.builder(ollamaChatModel)
                .defaultOptions(chatOptions)
                .defaultAdvisors(new SimpleLoggerAdvisor(), jdbcChatMemoryAdvisor)
                .defaultSystem("回答時請使用清楚、易理解且專業的繁體中文。")
                .build();
    }

    @Bean("openaiChatClient-inChatMemory")
    public ChatClient openaiChatClientInMemory(OpenAiChatModel openAiChatModel, @Qualifier("generic-inMemoryChatMemory") ChatMemory chatMemory) {
        ChatOptions.Builder chatOptions = ChatOptions.builder().temperature(0.5).maxTokens(500);
        Advisor inMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return ChatClient.builder(openAiChatModel)
                .defaultOptions(chatOptions)
                .defaultAdvisors(new SimpleLoggerAdvisor(), inMemoryAdvisor) // inMemoryAdvisor 加入預設 Advisor（攔截器）
                .defaultSystem("回答時請使用清楚、易理解且專業的繁體中文。")
                .build();
    }

    @Bean("openaiChatClient-jdbcChatMemory-toolCalling")
    public ChatClient openaiToolCalling(OpenAiChatModel openAiChatModel, @Qualifier("openai-jdbcChatMemory") ChatMemory chatMemory, TimeTool timeTool) {
        ChatOptions.Builder chatOptions = ChatOptions.builder().temperature(0.5).maxTokens(500);
        Advisor jdbcChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return ChatClient.builder(openAiChatModel)
                .defaultOptions(chatOptions)
                .defaultAdvisors(new SimpleLoggerAdvisor(), jdbcChatMemoryAdvisor)
                .defaultTools(timeTool)
                .defaultSystem("回答時請使用清楚、易理解且專業的繁體中文。")
                .build();
    }

    /*------------------------without ChatMemory------------------------*/
    @Bean("openaiChatClient-withoutMemory")
    public ChatClient openaiChatClientWithoutMemory(OpenAiChatModel openAiChatModel) {
        ChatOptions.Builder chatOptions = ChatOptions.builder().temperature(0.5).maxTokens(500);

        return ChatClient.builder(openAiChatModel)
                .defaultOptions(chatOptions)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultSystem("回答時請使用清楚、易理解且專業的繁體中文。")
                .build();
    }
}
