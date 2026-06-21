package com.example.mySpringAi.controller;

import com.openai.models.audio.AudioResponseFormat;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
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

@RestController
@RequestMapping("/audio")
public class AudioController {
    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;
    private final TextToSpeechModel speechModel; // OpenAiAudioSpeechModel implements TextToSpeechModel


    @Autowired
    public AudioController(OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel, OpenAiAudioSpeechModel speechModel) {
        this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
        this.speechModel = speechModel;
    }

    @GetMapping("/transcribe")
    String transcribe(@Value("classpath:SpringAI.mp3") Resource audioFile) {
        return openAiAudioTranscriptionModel.call(new AudioTranscriptionPrompt(audioFile))
                .getResult()
                .getOutput();
    }

    @GetMapping("/transcribe-options")
    String transcribeWithOptions(@Value("classpath:SpringAI.mp3") Resource audioFile) {
        // AudioTranscriptionPrompt 的用途是：把「音檔 + 轉錄設定（options）」包成一個完整的請求物件，交給 model 執行。
        AudioTranscriptionResponse audioTranscriptionResponse = openAiAudioTranscriptionModel.call(new AudioTranscriptionPrompt(
                audioFile,
                OpenAiAudioTranscriptionOptions.builder()
                        .prompt("Talking about Spring AI")
                        .language("en")
                        .temperature(0.5f)
                        .responseFormat(AudioResponseFormat.VTT).build()));
        return audioTranscriptionResponse.getResult().getOutput();
    }

    @GetMapping("/text-to-speech")
    String speech(@RequestParam("message") String message) throws IOException {
        byte[] audioBytes = speechModel.call(message);
        Path path = Paths.get("output.mp3");
        Files.write(path, audioBytes);
        return "MP3 saved successfully to " + path.toAbsolutePath();
    }

    @GetMapping("/text-to-speech-options")
    String spechWithOptions(@RequestParam("message") String message) throws IOException {
        TextToSpeechResponse speechResponse = speechModel.call(new TextToSpeechPrompt(message,
                OpenAiAudioSpeechOptions.builder().voice(OpenAiAudioSpeechOptions.Voice.NOVA) // 指定「語音合成（TTS）要用哪一種聲音（Voice）」
                        .speed(1.0)
                        .responseFormat(OpenAiAudioSpeechOptions.AudioResponseFormat.MP3).build()));
        Path path = Paths.get("speech-options.mp3");
        Files.write(path, speechResponse.getResult().getOutput());
        return "MP3 saved successfully to " + path.toAbsolutePath();
    }
}
