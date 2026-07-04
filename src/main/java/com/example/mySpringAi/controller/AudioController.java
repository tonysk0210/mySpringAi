package com.example.mySpringAi.controller;

import com.openai.models.audio.AudioResponseFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/audio")
public class AudioController {
    private final TranscriptionModel transcriptionModel;
    private final TextToSpeechModel textToSpeechModel; // OpenAiAudioSpeechModel implements TextToSpeechModel


    @Autowired
    public AudioController(TranscriptionModel transcriptionModel, TextToSpeechModel textToSpeechModel) {
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
    }

    /**
     * 把音檔轉成文字
     */
    @GetMapping("/transcribe")
    String transcribe(@Value("classpath:SpringAI.mp3") Resource audioFile) {
        log.info("Transcription 請求: {}", audioFile.getFilename());
        String result = transcriptionModel.call(new AudioTranscriptionPrompt(audioFile))
                .getResult()
                .getOutput();
        log.info("Transcription 回應: {}", result);
        return result;
    }

    /**
     * 把音檔轉成文字，但可以帶額外設定來控制轉錄行為
     * <p>
     * 流程拆解
     * 音檔 (SpringAI.mp3)
     * ↓
     * AudioTranscriptionPrompt（把音檔 + 選項包成請求）
     * ↓
     * transcriptionModel.call(...)（送給 OpenAI Whisper API）
     * ↓
     * AudioTranscriptionResponse（收到回應）
     * ↓
     * .getResult().getOutput()（取出純文字）
     */
    @GetMapping("/transcribe-options")
    String transcribeWithOptions(@Value("classpath:SpringAI.mp3") Resource audioFile) {
        log.info("Transcription 請求 (options): {}", audioFile.getFilename());
        // AudioTranscriptionPrompt 的用途是把「音檔 + 轉錄設定 options」包成一個完整請求，交給 transcriptionModel 執行。
        // 1. 建立 AudioTranscriptionPrompt 物件，把音檔和轉錄設定 options 打包起來
        AudioTranscriptionResponse audioTranscriptionResponse = transcriptionModel.call(new AudioTranscriptionPrompt(
                audioFile,
                OpenAiAudioTranscriptionOptions.builder()
                        .prompt("Talking about Spring AI") // 提供音檔主題/上下文，幫助模型辨識像 "Spring AI" 這類專有名詞
                        .language("en") // 明確指定音檔語言是英文，避免模型自行偵測語言
                        .temperature(0.5f) // 控制轉錄結果的不確定性；0 較保守，較高值可能產生較多變化
                        .responseFormat(AudioResponseFormat.VTT).build())); // 回傳 WebVTT 字幕格式，通常包含時間戳記，不是單純純文字

        // 2. 取出轉錄結果
        String result = audioTranscriptionResponse.getResult().getOutput();
        log.info("Transcription 回應 (options): {}", result);
        return result;
    }

    /**
     * 把文字轉成音檔
     */
    @GetMapping("/text-to-speech")
    String speech(@RequestParam("message") String message) throws IOException {
        log.info("TTS 請求: {}", message);
        // 1. 呼叫 textToSpeechModel 的 call 方法，傳入要轉成音檔的文字
        byte[] audioBytes = textToSpeechModel.call(message);

        // 2. 將音檔保存到本地
        Path path = Paths.get("audio-output", "speech.mp3");

        // 3. 寫入音檔
        Files.write(path, audioBytes);

        log.info("TTS 完成，檔案儲存至: {}", path.toAbsolutePath());
        // 4. 回應結果
        return "MP3 已成功儲存至：" + path.toAbsolutePath();
    }

    @GetMapping("/text-to-speech-options")
    String speechWithOptions(@RequestParam("message") String message) throws IOException {
        log.info("TTS 請求 (options): {}", message);
        // 1. 建立 TextToSpeechPrompt，把文字和語音合成 options 打包成請求
        TextToSpeechResponse speechResponse = textToSpeechModel.call(new TextToSpeechPrompt(message,
                OpenAiAudioSpeechOptions.builder().voice(OpenAiAudioSpeechOptions.Voice.NOVA) // 指定 TTS 使用 NOVA 聲音
                        .speed(1.0) // 語速倍率；1.0 代表正常速度
                        .responseFormat(OpenAiAudioSpeechOptions.AudioResponseFormat.MP3).build())); // 要求回傳 MP3 音訊格式

        // 2. 建立輸出檔案路徑
        Path path = Paths.get("audio-output", "speech-options.mp3");

        // 3. 從回應取出音訊 bytes，寫入 MP3 檔案
        Files.write(path, speechResponse.getResult().getOutput());
        log.info("TTS 完成 (options)，檔案儲存至: {}", path.toAbsolutePath());
        // 4. 回應結果
        return "MP3 已成功儲存至：" + path.toAbsolutePath();
    }
}
