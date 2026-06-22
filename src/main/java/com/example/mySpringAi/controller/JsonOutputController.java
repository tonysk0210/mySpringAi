package com.example.mySpringAi.controller;

import com.example.mySpringAi.dto.CountryCitiesDto;
import com.example.mySpringAi.payload.JsonOutputPayload;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class JsonOutputController {

    private final ChatClient openaiChatClientWithoutMemory;

    @Autowired
    public JsonOutputController(@Qualifier("openaiChatClient-withoutMemory") ChatClient openaiChatClientWithoutMemory) {
        this.openaiChatClientWithoutMemory = openaiChatClientWithoutMemory;
    }

    // openai - convert llm text to JSON DTO

    /**
     * 讓 Spring AI 要求模型輸出符合 CountryCitiesDto 結構的內容，然後用 converter 把模型回傳文字轉成 CountryCitiesDto 物件。
     */
    @PostMapping("/openai/generateJsonDto")
    public ResponseEntity<CountryCitiesDto> openaiGenerateJsonDto(@RequestBody JsonOutputPayload jsonOutputPayload) {

        // 呼叫 OpenAI 生成符合 CountryCitiesDto 結構的 JSON 數據
        CountryCitiesDto dto = openaiChatClientWithoutMemory.prompt()
                .user(jsonOutputPayload.message())
                .call()
                .entity(CountryCitiesDto.class); // 要求 LLM 回傳符合 CountryCitiesDto 結構的內容，並轉成 DTO 物件。
        return ResponseEntity.ok(dto);
    }

    // openai - convert llm text to List in json form; LLM 自己決定 JSON 長什麼樣
    @PostMapping("/openai/generateList")
    public ResponseEntity<List<String>> openaiGenerateList(@RequestBody JsonOutputPayload jsonOutputPayload) {
        List<String> list = openaiChatClientWithoutMemory.prompt()
                .user(jsonOutputPayload.message())
                .call()
                .entity(new ListOutputConverter()); // content to json List
        return ResponseEntity.ok(list);
    }

    // openai - convert llm text to Map in json form; LLM 自己決定 JSON 長什麼樣
    @PostMapping("/openai/generateMap")
    public ResponseEntity<Map<String, Object>> openaiGenerateMap(@RequestBody JsonOutputPayload jsonOutputPayload) {
        Map<String, Object> map = openaiChatClientWithoutMemory.prompt()
                .user(jsonOutputPayload.message())
                .call()
                .entity(new MapOutputConverter()); // content to json List
        return ResponseEntity.ok(map);
    }

    // openai - convert llm text to List of dto in json form; LLM 自己決定 JSON 長什麼樣
    @PostMapping("/openai/generateListJsonDto")
    public ResponseEntity<List<CountryCitiesDto>> openaiGenerateListJsonDto(@RequestBody JsonOutputPayload jsonOutputPayload) {
        List<CountryCitiesDto> listDto = openaiChatClientWithoutMemory.prompt()
                .user(jsonOutputPayload.message())
                .call()
                .entity(new ParameterizedTypeReference<List<CountryCitiesDto>>() {
                }); // content to json List
        return ResponseEntity.ok(listDto);
    }
}
