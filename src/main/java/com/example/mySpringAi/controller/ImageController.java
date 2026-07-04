package com.example.mySpringAi.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/image")
public class ImageController {

    private final ImageModel imageModel;

    @Autowired
    public ImageController(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    /**
     * 產生圖片（B64 格式），decode 後存至 ./image-output/image.png
     */
    @GetMapping("/image")
    String generateImage(@RequestParam("message") String message) throws IOException {
        log.info("圖片生成請求: {}", message);

        // 1. 呼叫 imageModel 的 call 方法，傳入 ImagePrompt
        ImageResponse imageResponse = imageModel.call(new ImagePrompt(message));

        // 2. 從 imageResponse 中取得圖片的 B64 字串
        String b64Json = imageResponse.getResults().get(0).getOutput().getB64Json();

        // 3. 將 B64 字串解碼為 byte[]，並儲存至 ./image-output/image.png
        byte[] imageBytes = Base64.getDecoder().decode(b64Json);
        Path path = Paths.get("image-output", "image.png");
        Files.write(path, imageBytes);
        log.info("圖片已儲存至: {}", path.toAbsolutePath());
        return "圖片已成功儲存至：" + path.toAbsolutePath();
    }

    /**
     * 產生圖片（B64 格式），decode 後存至 ./image-output/image-options.png
     */
    @GetMapping("/image-options")
    String generateImageWithOptions(@RequestParam("message") String message) throws IOException {
        log.info("圖片生成請求 (options): {}", message);

        // 1. 呼叫 imageModel 的 call 方法，傳入 ImagePrompt
        ImageResponse imageResponse = imageModel.call(new ImagePrompt(message,
                OpenAiImageOptions.builder()
                        .n(1)
                        .quality("low") // gpt-image-2 支援 low / medium / high，不支援 hd
                        .size("1024x1024") // gpt-image-2 用 size 字串，不用 height/width 分開設
                        .model("gpt-image-2").build()));

        // 2. 從 imageResponse 中取得圖片的 B64 字串
        String b64Json = imageResponse.getResult().getOutput().getB64Json();

        // 3. 將 B64 字串解碼為 byte[]，並儲存至 ./image-output/image-options.png
        byte[] imageBytes = Base64.getDecoder().decode(b64Json);
        Path path = Paths.get("image-output", "image-options.png");
        Files.write(path, imageBytes);
        log.info("圖片已儲存至: {}", path.toAbsolutePath());
        return "圖片已成功儲存至：" + path.toAbsolutePath();
    }
}
