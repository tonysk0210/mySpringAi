package com.example.mySpringAi.tools;

import com.example.mySpringAi.entity.HelpDeskTicketEntity;
import com.example.mySpringAi.payload.HelpDeskTicketPayload;
import com.example.mySpringAi.service.HelpDeskTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HelpDeskTicketTool {

    private final HelpDeskTicketService service;

    // 內部 framework + reflection so Tool's methods still gets called even not declared as public
    @Tool(name = "createTicket", description = "Create a HelpDesk support ticket", returnDirect = true)
    String createTicket(@ToolParam(description = "Details to create a Support ticket") HelpDeskTicketPayload payload, ToolContext toolContext) {

        // ToolContext 提供當前 Tool 呼叫的上下文（如 userName / headers），只在這次 call 中有效
        String username = (String) toolContext.getContext().get("userName");
        log.info("Creating support ticket for user: {} with details: {}", username, payload);

        // 將 LLM 填入的 payload + 呼叫者 username 寫入資料庫
        HelpDeskTicketEntity savedTicket = service.createHelpDeskTicket(payload, username);
        log.info("Ticket created successfully. Ticket ID: {}, UserName: {}", savedTicket.getId(), savedTicket.getUsername());

        // returnDirect=true：模型會直接回傳此字串給使用者，不再追加其他回答
        return "Ticket id#: " + savedTicket.getId() + " created successfully for user: " + savedTicket.getUsername();
    }

    @Tool(description = "Fetch the status of the tickets based on a given userName")
    List<HelpDeskTicketEntity> getTicketStatus(ToolContext toolContext) {
        // 一樣由 ToolContext 拿出呼叫者 username，避免讓模型自由填寫
        String username = (String) toolContext.getContext().get("userName");
        log.info("Fetching tickets for user: {}", username);

        // 查詢該使用者所有工單並回傳；模型可用此結果回答進度
        List<HelpDeskTicketEntity> tickets = service.getHelpDeskTicketsByUser(username);
        log.info("Found {} tickets for user: {}", tickets.size(), username);

        // throw new RuntimeException("Unable to fetch ticket status");
        return tickets;
    }
}
