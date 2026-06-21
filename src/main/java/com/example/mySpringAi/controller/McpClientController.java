package com.example.mySpringAi.controller;

import com.example.mySpringAi.payload.GenericChatPayload;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;


@RestController
@RequestMapping("/mcp")
@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = "enabled", havingValue = "true") // 只有在 application.properties 或 application.yml 中設定 spring.ai.mcp.client.enabled=true 時，這個 Controller 才會被註冊到 Spring Context 中
public class McpClientController {

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider; // 提供「Tool → Callback」對應關係的物件 [mcpToolCallbacks]

    @Autowired
    public McpClientController(@Qualifier("openaiChatClient-jdbcChatMemory") ChatClient chatClient, ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chatClient;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    @PostMapping("/mcpchat")
    public ResponseEntity<String> chat(@RequestBody GenericChatPayload payload, @RequestHeader("userName") String userName) {
        String response = chatClient.prompt()
                .advisors(aSpec -> aSpec.param(CONVERSATION_ID, "mcp-" + userName))
                .toolCallbacks(toolCallbackProvider) // LLM 可以使用 這個 ToolCallbackProvider 裡面定義的所有工具」
                .user(payload.message())
                .call().content();
        return ResponseEntity.ok(response);
    }

    @Value("classpath:/promptTemplate/HelpDeskTicketPromptTemplate.st")
    Resource helpDeskTicketPromptTemplate;

    @PostMapping("/mcpServer")
    public ResponseEntity<String> mcpServer(@RequestBody GenericChatPayload payload, @RequestHeader("userName") String userName) {
        String response = chatClient.prompt()
                .system(helpDeskTicketPromptTemplate)
                .advisors(aSpec -> aSpec.param(CONVERSATION_ID, "mcpServer-" + userName)) //this is for chat memory
                .toolCallbacks(toolCallbackProvider) // LLM 可以使用 這個 ToolCallbackProvider 裡面定義的所有工具」
                .user(payload.message() + ". My userName is " + userName) //this userName is for sending to MCP server
                .call().content();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mcpServerRemote")
    public ResponseEntity<String> mcpServerRmoete(@RequestBody GenericChatPayload payload, @RequestHeader("userName") String userName) {
        String response = chatClient.prompt()
                .system(helpDeskTicketPromptTemplate)
                .advisors(aSpec -> aSpec.param(CONVERSATION_ID, "mcpServerRemote-" + userName)) //this is for chat memory
                .toolCallbacks(toolCallbackProvider) // LLM 可以使用 這個 ToolCallbackProvider 裡面定義的所有工具」includes: the ones defined in mcp-servers.json (stdio) and (see)
                .user(payload.message() + ". My userName is " + userName) //this userName is for sending to MCP server
                .call().content();
        return ResponseEntity.ok(response);
    }
}
