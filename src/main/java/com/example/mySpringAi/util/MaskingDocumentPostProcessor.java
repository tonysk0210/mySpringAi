package com.example.mySpringAi.util;

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

    // Email pattern 允許沒有 TLD 的內部帳號格式，例如 tutor@eazybytes
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+(?:\\.[A-Za-z]{2,})?\\b", Pattern.CASE_INSENSITIVE);
    // Phone pattern 支援 123-456-7890、(123) 456-7890、+1 123-456-7890 等格式
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b", Pattern.CASE_INSENSITIVE);

    // 機敏資訊遮罩
    private static final String EMAIL_REPLACEMENT = "[機敏資訊遮罩_EMAIL]";
    private static final String PHONE_REPLACEMENT = "[機敏資訊遮罩_PHONE]";

    private MaskingDocumentPostProcessor() {
    }

    @Override
    public List<Document> process(Query query, List<Document> documents) { // Query: 一次 RAG 查詢的上下文物件

        // 1. 明確定義 process 的輸入合約，避免後面 query.text()、documents.stream()、document.getText() 發生不清楚的 NullPointerException
        Assert.notNull(query, "query 不可為 null");
        Assert.notNull(documents, "documents 不可為 null");
        if (CollectionUtils.isEmpty(documents)) {
            return documents;
        }
        Assert.noNullElements(documents, "documents 不可包含 null 元素");

        log.info("針對 {} 筆 documents 執行機敏資訊遮罩", documents.size());

        // 2. 逐一處理每個 retrieved Document，將 text 中的 email / phone number 遮罩後，建立新的 Document 回傳
        return documents.stream().map(document -> {

            // 2.1 取得原本 Document 的文字內容。
            String text = document.getText() != null ? document.getText() : "";

            // 2.2 對文件文字執行 email 遮罩與電話號碼遮罩。
            String maskedText = maskSensitiveInformation(text);

            // 2.3 Document 不直接原地修改；使用 mutate() 保留原 metadata，並覆寫 text / 新增 pii_masked 標記
            return document.mutate() // 這份舊 Document 當範本，準備做一份修改後的新 Document
                    .text(maskedText) // 把新 Document 的 text 改成遮罩後文字。
                    .metadata("pii_masked", true) // 在新 Document 的 metadata 裡加一個標記
                    .build();
        }).toList();

        /*
        原本的 document 可能長這樣：
        Document A
        text:
          Please contact hr@eazybytes.com or call 123-456-7890

        metadata:
          source = Eazybytes_HR_Policies.pdf
          page = 3

        結果會建立一個新的 Document：
        Document B
        text:
          Please contact [機敏資訊遮罩_EMAIL] or call [機敏資訊遮罩_PHONE]

        metadata:
          source = Eazybytes_HR_Policies.pdf
          page = 3
          pii_masked = tru
        */
    }

    private String maskSensitiveInformation(String text) {
        String masked = text;
        masked = EMAIL_PATTERN.matcher(masked).replaceAll(EMAIL_REPLACEMENT); // 先遮 Email
        masked = PHONE_PATTERN.matcher(masked).replaceAll(PHONE_REPLACEMENT); // 再用「遮完 Email 的結果」去遮 Phone
        return masked;
    }

    public static MaskingDocumentPostProcessor getInstance() {
        return new MaskingDocumentPostProcessor();
    }
}
