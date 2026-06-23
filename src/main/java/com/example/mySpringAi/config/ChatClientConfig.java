package com.example.mySpringAi.config;

import com.example.mySpringAi.advisor.TokenUsageAuditAdvisor;
import com.example.mySpringAi.tools.TimeTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 建立 4 種 ChatClient  bean，供不同場景使用。
 */

@Configuration
public class ChatClientConfig {

    // 使用 OpenAI 模型、但沒有聊天記憶功能的 ChatClient bean，提供給不需要記憶功能的場景使用（例如單次問答、工具調用等）。
    @Bean("openaiChatClient-withoutMemory")
    public ChatClient openaiChatClientWithoutMemory(OpenAiChatModel openAiChatModel) {

        // 1. 設定模型用的「參數」
        ChatOptions.Builder<OpenAiChatOptions.Builder> chatOptions = OpenAiChatOptions.builder().temperature(0.5).maxTokens(500);

        // 2. 建立 ChatClient 並加入 2 種 advisor：TokenUsageAuditAdvisor、SimpleLoggerAdvisor
        return ChatClient.builder(openAiChatModel)
                .defaultOptions(chatOptions)
                .defaultAdvisors(new TokenUsageAuditAdvisor(), new SimpleLoggerAdvisor()) // 加入 SimpleLoggerAdvisor，讓此 ChatClient 自動記錄對話過程。
                .defaultSystem("回答時請使用清楚、易理解且專業的繁體中文。")
                .build();
    }

    //  使用 OpenAI 模型，並搭配 in-memory chat memory 來記住對話上下文。
    @Bean("openaiChatClient-inChatMemory")
    public ChatClient openaiChatClientInMemory(OpenAiChatModel openAiChatModel, @Qualifier("inMemoryChatMemory") ChatMemory chatMemory) {

        // 1. 設定模型用的「參數」
        ChatOptions.Builder<OpenAiChatOptions.Builder> chatOptions = OpenAiChatOptions.builder().temperature(0.5).maxTokens(500);

        // 2. 建立 MessageChatMemoryAdvisor，也就是「會話記憶攔截器」。使用 inMemoryChatMemory 作為記憶源
        Advisor inMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        // 3. 建立 ChatClient 並加入 3 種 advisor：TokenUsageAuditAdvisor、SimpleLoggerAdvisor、inMemoryAdvisor
        return ChatClient.builder(openAiChatModel)
                .defaultOptions(chatOptions)
                .defaultAdvisors(new TokenUsageAuditAdvisor(), new SimpleLoggerAdvisor(), inMemoryAdvisor)
                .defaultSystem("回答時請使用清楚、易理解且專業的繁體中文。")
                .build();
    }

    // 使用 OpenAI 模型，並搭配 jdbc chat memory 來記住對話上下文。
    @Bean("openaiChatClient-jdbcChatMemory")
    public ChatClient openaiChatClient(OpenAiChatModel openAiChatModel, @Qualifier("jdbcChatMemory") ChatMemory chatMemory) {
        // 1. 設定模型用的「參數」
        ChatOptions.Builder<OpenAiChatOptions.Builder> chatOptions = OpenAiChatOptions.builder().temperature(0.5).maxTokens(500);

        // 2. 建立 MessageChatMemoryAdvisor，也就是「會話記憶攔截器」。使用 jdbcChatMemory 作為記憶源
        Advisor jdbcChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        // 3. 建立 ChatClient 並加入 3 種 advisor：TokenUsageAuditAdvisor、SimpleLoggerAdvisor、jdbcChatMemoryAdvisor
        return ChatClient.builder(openAiChatModel)
                .defaultOptions(chatOptions)
                .defaultAdvisors(new TokenUsageAuditAdvisor(), new SimpleLoggerAdvisor(), jdbcChatMemoryAdvisor)
                .defaultSystem("回答時請使用清楚、易理解且專業的繁體中文。")
                .build();
    }

    // 使用 Ollama 模型，並搭配 jdbc chat memory 來記住對話上下文。
    @Bean("ollamaChatClient-jdbcChatMemory")
    public ChatClient ollmaChatClient(OllamaChatModel ollamaChatModel, @Qualifier("jdbcChatMemory") ChatMemory chatMemory) {

        // 1. 設定模型用的「參數」
        ChatOptions.Builder<OllamaChatOptions.Builder> chatOptions = OllamaChatOptions.builder().temperature(0.5).maxTokens(500);

        // 2. 建立 MessageChatMemoryAdvisor，也就是「會話記憶攔截器」。使用 jdbcChatMemory 作為記憶源
        Advisor jdbcChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        // 3. 建立 ChatClient 並加入 3 種 advisor：TokenUsageAuditAdvisor、SimpleLoggerAdvisor、jdbcChatMemoryAdvisor
        return ChatClient.builder(ollamaChatModel)
                .defaultOptions(chatOptions)
                .defaultAdvisors(new TokenUsageAuditAdvisor(), new SimpleLoggerAdvisor(), jdbcChatMemoryAdvisor)
                .defaultSystem("回答時請使用清楚、易理解且專業的繁體中文。")
                .build();
    }


    @Bean("openaiChatClient-jdbcChatMemory-toolCalling")
    public ChatClient openaiToolCalling(OpenAiChatModel openAiChatModel, @Qualifier("jdbcChatMemory") ChatMemory chatMemory, TimeTool timeTool) {
        ChatOptions.Builder<OpenAiChatOptions.Builder> chatOptions = OpenAiChatOptions.builder().temperature(0.5).maxTokens(500);
        Advisor jdbcChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return ChatClient.builder(openAiChatModel)
                .defaultOptions(chatOptions)
                .defaultAdvisors(new TokenUsageAuditAdvisor(), new SimpleLoggerAdvisor(), jdbcChatMemoryAdvisor)
                .defaultTools(timeTool)
                .defaultSystem("回答時請使用清楚、易理解且專業的繁體中文。")
                .build();
    }
}


/**
 * note:
 * 1. OpenAiChatModel 和 OllamaChatModel 是 Spring 自動建立的 Bean 在 pom.xml 引入，但因為此專案同時有多個 ChatModel 實作，所以這裡直接注入具體型別避免歧義
 * spring-ai-starter-model-openai 會自動建立 OpenAiChatModel Bean。
 * spring-ai-starter-model-ollama 會自動建立 OllamaChatModel Bean。
 * <p>
 * 2. ChatClient.create 不會預設配置 memory；memory 需要透過 advisor 加入，通常用 builder 設成 defaultAdvisors 比較適合。
 * <p>
 * 3. TokenUsageAuditAdvisor 會計算對話的 token 數量並記錄下來，方便後續統計。SimpleLoggerAdvisor 會記錄對話過程到 log 文件中，方便追蹤問題。
 */
