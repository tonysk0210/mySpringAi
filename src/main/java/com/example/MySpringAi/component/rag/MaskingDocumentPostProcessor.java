package com.example.mySpringAi.component.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.regex.Pattern;

// Factory class
@Slf4j
public class MaskingDocumentPostProcessor implements DocumentPostProcessor {

    // Regex patterns for common PII
    // 原 pattern 要求有 TLD（如 .com），導致 tutor@eazybytes 這類無 TLD 的域名不會被遮罩；改為 TLD 可選
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+(?:\\.[A-Za-z]{2,})?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b", Pattern.CASE_INSENSITIVE);

    // Replacement strings for PII
    private static final String EMAIL_REPLACEMENT = "[REDACTED_EMAIL]";
    private static final String PHONE_REPLACEMENT = "[REDACTED_PHONE]";

    private MaskingDocumentPostProcessor() {
    }

    @Override
    public List<Document> process(Query query, List<Document> documents) { // Query: 一次 RAG 查詢的上下文物件
        Assert.notNull(query, "query cannot be null");
        Assert.notNull(documents, "documents cannot be null");
        Assert.noNullElements(documents, "documents cannot contain null elements");

        if (CollectionUtils.isEmpty(documents)) {
            return documents;
        }

        log.debug("Masking sensitive information in documents for query: {}", query.text());

        return documents.stream().map(document -> {
            String text = document.getText() != null ? document.getText() : "";
            // Apply PII masking
            String maskedText = maskSensitiveInformation(text); // 對文件文字執行 email 正則與電話號碼正則。
            // Document 是 immutable
            return document.mutate() // 回傳一個 Builder / Mutator
                    .text(maskedText) // 設定「新文件」的 text
                    .metadata("pii_masked", true) // 設定「新文件」的 metadata
                    .build();
        }).toList(); // 把 Stream 收集回 List<Document>
    }

    private String maskSensitiveInformation(String text) {
        String masked = text;
        // Mask emails
        masked = EMAIL_PATTERN.matcher(masked).replaceAll(EMAIL_REPLACEMENT); // 先遮 Email
        // Mask phone numbers
        masked = PHONE_PATTERN.matcher(masked).replaceAll(PHONE_REPLACEMENT); // 再用「遮完 Email 的結果」去遮 Phone
        return masked;
    }

    public static MaskingDocumentPostProcessor getInstance() {
        return new MaskingDocumentPostProcessor();
    }
}
