package com.example.mySpringAi.controller;

import com.example.mySpringAi.payload.MessageChatPayload;
import com.example.mySpringAi.tools.HelpDeskTicketTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("classpath:/promptTemplate/HelpDeskTicketPromptTemplate.st")
    Resource helpDeskTicketPromptTemplate;

    @Autowired
    public ToolCallingController(@Qualifier("openaiCCJdbcMemoryWithToolCalling") ChatClient chatClientTimeCalling,
                                 HelpDeskTicketTool helpDeskTicketTool) {
        this.chatClientTimeCalling = chatClientTimeCalling;
        this.helpDeskTicketTool = helpDeskTicketTool;
    }

    @PostMapping("/time")
    public ResponseEntity<String> time(@RequestBody MessageChatPayload payload, @RequestHeader("userName") String userName) {

        // 調用時間工具
        String response = chatClientTimeCalling.prompt()
                .advisors(aSpec -> aSpec.param(CONVERSATION_ID, "tool-" + userName))
                .user(payload.message())
                .call().content();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/helpDeskTicket")
    public ResponseEntity<String> helpDeskTicket(@RequestBody MessageChatPayload payload, @RequestHeader("userName") String userName) {
        String response = chatClientTimeCalling.prompt()
                .system(helpDeskTicketPromptTemplate)
                .tools(helpDeskTicketTool) // 註冊 HelpDeskTicketTool，本次呼叫可建立工單或查詢工單狀態；會疊加 defaultTools()
                .toolContext(Map.of("userName", userName)) // 傳入 userName 給 HelpDeskTicketTool
                .advisors(aSpec -> aSpec.param(CONVERSATION_ID, "toolHelpDeskTicket-" + userName))
                .user(payload.message())
                .call().content();
        // returnDirect=true 的 tool 結果會被 Spring AI 用 ObjectMapper 序列化成 JSON string
        // 這裡把它反序列化回純字串，換行符號才能正常顯示
        String body = unwrapJsonString(response);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    // 反序列化 JSON string
    private String unwrapJsonString(String s) {
        if (s != null && s.startsWith("\"")) {
            try {
                return objectMapper.readValue(s, String.class);
            } catch (Exception ignored) {
            }
        }
        return s;
    }
}
