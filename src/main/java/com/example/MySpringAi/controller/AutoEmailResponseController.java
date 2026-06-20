package com.example.mySpringAi.controller;

import com.example.mySpringAi.payload.AutoEmailResponsePayload;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AutoEmailResponseController {

    private final ChatClient openaiChatClientWithoutMemory;

    /**
     * 這個方法會使用 Spring AI 的 PromptTemplate 來生成 Prompt，然後使用 ChatClient 來生成對應的 Response
     */
    @Value("classpath:promptTemplate/AutoEmailResponsePromptTemplate.st")
    private Resource emailResponsePromptTemplateText;

    @Autowired
    public AutoEmailResponseController(@Qualifier("openaiChatClient-withoutMemory") ChatClient openaiChatClientWithoutMemory) {
        this.openaiChatClientWithoutMemory = openaiChatClientWithoutMemory;
    }

    //openai auto-generate email response given customer name & customer concern
    @PostMapping("/openai/emailResponse")
    public String openaiEmailResponse(@RequestBody AutoEmailResponsePayload autoEmailResponsePayload) {
        return openaiChatClientWithoutMemory.prompt()
                .user(promptUserSpec -> promptUserSpec.text(emailResponsePromptTemplateText)
                        .param("customerName", autoEmailResponsePayload.customerName())
                        .param("customerMessage", autoEmailResponsePayload.customerMessage()))
                .call().content();
    }
}
