package com.example.mySpringAi.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;

@Slf4j
@Component
public class TimeTool {

    /**
     * 想要 tool 結果原封不動回傳 -> returnDirect = true
     * 想要 LLM 幫忙整理 tool 結果 -> 不要 returnDirect = true
     * <p>
     * ┌──────────────┬─────────────────────┬──────────────────────┐
     * │              │ returnDirect = true │         預設        　│
     * ├──────────────┼─────────────────────┼──────────────────────┤
     * │ LLM 呼叫次數 　│ 1 次                │ 2 次                　│
     * ├──────────────┼─────────────────────┼──────────────────────┤
     * │ 回應內容     　│ 工具原始輸出   　    　│ LLM 整理後的自然語言    │
     * ├──────────────┼─────────────────────┼──────────────────────┤
     * │ 適用場景      │ 需要精確原始值         │ 需要人性化格式          │
     * └──────────────┴─────────────────────┴──────────────────────┘
     */

    // returnDirect = true：Tool 執行結果會直接作為 ChatClient 最終回應，不再讓 LLM 根據 tool 結果續寫答案。
    @Tool(name = "getCurrentLocalTime", description = "取得當前本地時間", returnDirect = true) // 把一個 Java 方法註冊成「可被 LLM 呼叫的工具（Tool）」
    public String getCurrentLocalTime() {
        log.info("呼叫 Tool - getCurrentLocalTime");
        return "現在時間是: " + LocalTime.now().toString();
    }

    // @ToolParam：需要 LLM 提供 IANA time zone 參數，Tool 結果會回交給 LLM，由 LLM 整理成最終回答。
    @Tool(name = "getCurrentTime", description = "取得指定時區的當前時間")
    public String getCurrentTime(@ToolParam(description = "IANA time zone, e.g. Asia/Taipei, UTC, Europe/London") String timeZone) {
        log.info("呼叫 Tool - getCurrentTime: {}", timeZone);
        return LocalTime.now(ZoneId.of(timeZone)).toString();
    }
}
