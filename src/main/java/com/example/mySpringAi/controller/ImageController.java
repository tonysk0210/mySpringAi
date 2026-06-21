package com.example.mySpringAi.controller;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/image")
public class ImageController {

    private final ImageModel imageModel;

    @Autowired
    public ImageController(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    @GetMapping("/image")
    String genateImage(@RequestParam("message") String message) {
        ImageResponse imageResponse = imageModel.call(new ImagePrompt(message));
        return imageResponse.getResult().getOutput().getUrl();
    }

    @GetMapping("/image-options")
    String genateImageWithOptions(@RequestParam("message") String message) {
        var imageResponse = imageModel.call(new ImagePrompt(message,
                OpenAiImageOptions.builder()
                        .n(1) //Number of images
                        .quality("hd")
                        .style("natural")
                        .height(1024)
                        .width(1024).responseFormat("url").build()));
        return imageResponse.getResult().getOutput().getUrl();
    }

}
