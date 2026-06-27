package com.example.mySpringAi.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

@Slf4j
public class PrettyLoggerAdvisor implements CallAdvisor {

    private static final String BAR        = "═".repeat(50);
    // ║ + space + %-13s label + space = 16 chars，continuation indent 對齊
    private static final String CONT       = "║               ";

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        logRequest(request);
        ChatClientResponse response = chain.nextCall(request);
        logResponse(response);
        return response;
    }

    // ────────────────────────────────────────────────────────────
    // Request
    // ────────────────────────────────────────────────────────────

    private void logRequest(ChatClientRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══ ► LLM Request ").append(BAR).append("\n");

        for (Message message : request.prompt().getInstructions()) {
            switch (message.getMessageType()) {
                case SYSTEM -> appendSection(sb, "[SYSTEM]", message.getText());
                case USER   -> appendUserMessage(sb, message.getText());
                default     -> appendSection(sb, "[" + message.getMessageType() + "]", message.getText());
            }
        }

        appendDocs(sb, request.context());
        sb.append("╚").append(BAR).append("══════════════════");
        log.debug(sb.toString());
    }

    private void appendUserMessage(StringBuilder sb, String text) {
        appendSection(sb, "[USER]", text);
    }

    private void appendDocs(StringBuilder sb, Map<String, Object> context) {
        List<Document> docs = getRagDocuments(context);
        if (docs.isEmpty()) return;

        sb.append(String.format("║ %-13s ", "[DOCS]")).append(docs.size()).append(" 筆\n");
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            sb.append(String.format("%s#%d  chunk_index=%-3s  score=%.4f  %s%n",
                    CONT, i + 1,
                    doc.getMetadata().get("chunk_index"),
                    doc.getScore(),
                    doc.getMetadata().get("source")));
        }
    }

    // ────────────────────────────────────────────────────────────
    // Response
    // ────────────────────────────────────────────────────────────

    private void logResponse(ChatClientResponse response) {
        String text = response.chatResponse().getResult().getOutput().getText();
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══ ◄ LLM Response ").append(BAR).append("\n");
        appendSection(sb, "[ANSWER]", text != null ? text : "(no text)");
        sb.append("╚").append(BAR).append("══════════════════");
        log.debug(sb.toString());
    }

    // ────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────

    // 按換行拆成多行，第一行用 label 前綴，後續行用 CONT 縮排對齊
    private void appendSection(StringBuilder sb, String label, String content) {
        String firstPrefix = String.format("║ %-13s ", label);
        String[] lines = content.split("[\\r\\n]+");
        boolean first = true;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            sb.append(first ? firstPrefix : CONT).append(line).append("\n");
            first = false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Document> getRagDocuments(Map<String, Object> context) {
        Object docs = context.get("rag_document_context");
        return docs instanceof List<?> list ? (List<Document>) list : List.of();
    }

    @Override
    public String getName() {
        return "PrettyLoggerAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
