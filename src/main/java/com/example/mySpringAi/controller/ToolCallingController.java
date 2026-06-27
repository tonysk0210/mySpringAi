package com.example.mySpringAi.controller;

import com.example.mySpringAi.payload.GenericChatPayload;
import com.example.mySpringAi.tools.HelpDeskTicketTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Controller
@RequestMapping("/tool")
public class ToolCallingController {

    private final ChatClient chatClientTimeCalling;
    private final HelpDeskTicketTool helpDeskTicketTool;

    @Value("classpath:/promptTemplate/HelpDeskTicketPromptTemplate.st")
    Resource helpDeskTicketPromptTemplate;

    @Autowired
    public ToolCallingController(@Qualifier("openaiChatClient-jdbcChatMemory-toolCalling") ChatClient chatClientTimeCalling,
                                 HelpDeskTicketTool helpDeskTicketTool) {
        this.chatClientTimeCalling = chatClientTimeCalling;
        this.helpDeskTicketTool = helpDeskTicketTool;
    }

    @PostMapping("/time")
    public ResponseEntity<String> time(@RequestBody GenericChatPayload payload, @RequestHeader("userName") String userName) {

        // 調用時間工具
        String response = chatClientTimeCalling.prompt()
                .advisors(aSpec -> aSpec.param(CONVERSATION_ID, "tool-" + userName))
                .user(payload.message())
                .call().content();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/helpDeskTicket")
    public ResponseEntity<String> helpDeskTicket(@RequestBody GenericChatPayload payload, @RequestHeader("userName") String userName) {
        String response = chatClientTimeCalling.prompt()
                .system(helpDeskTicketPromptTemplate)        // System Prompt：指示 LLM 如何處理工單
                .tools(helpDeskTicketTool)                   // 註冊可呼叫的 tool（新增工單）疊加 .defaultTools()
                .toolContext(Map.of("userName", userName))   // 傳遞上下文給 tool，例如當前使用者
                .advisors(aSpec -> aSpec.param(CONVERSATION_ID, "toolHelpDeskTicket-" + userName)) // 以 userName 隔離對話記憶
                .user(payload.message())                     // 使用者輸入
                .call().content();                           // 觸發模型與工具呼叫，取得內容
        return ResponseEntity.ok(response);
    }
}
