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

    // returnDirect = true Tool 的回傳值「直接成為最終回應」，不再交回給 LLM 繼續生成。
    @Tool(name = "getCurrentLocalTime", description = "get the current local time", returnDirect = true) // 把一個 Java 方法註冊成「可被 LLM 呼叫的工具（Tool）」
    public String getCurrentLocalTime() {
        log.info("Returning current local time");
        return "現在時間是: " + LocalTime.now().toString();
    }

    @Tool(name = "getCurrentTime", description = "get the current time in a specific time zone")
    public String getCurrentTime(@ToolParam(description = "IANA time zone, e.g. Asia/Taipei, UTC, Europe/London") String timeZone) {
        log.info("Returning current time in time zone: {}", timeZone);
        return LocalTime.now(ZoneId.of(timeZone)).toString();
    }
}
