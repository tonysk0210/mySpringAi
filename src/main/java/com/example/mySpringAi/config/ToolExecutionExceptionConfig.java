package com.example.mySpringAi.config;

import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolExecutionExceptionConfig {

    /**
     * 註冊 tool 例外處理器；當 tool 執行失敗時，直接重新拋出例外，
     * alwaysThrow=true 設計上是要 re-throw 讓例外傳到 controller，但根據 log 顯示，Spring AI 2.0 的 ToolCallingManager 在更上層仍然攔截並處理掉了。
     * 這個 config 目前對你的專案而言是無效的 dead config
     */
    @Bean
    ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return new DefaultToolExecutionExceptionProcessor(true);
    }
}
