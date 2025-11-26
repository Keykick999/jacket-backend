package com.example.ThirdProject.ai.controller;

import com.example.ThirdProject.ai.dto.AiImageRequest;
import com.example.ThirdProject.ai.dto.AiImageResponse;
import com.example.ThirdProject.ai.service.AiImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiImageService aiImageService;

    @PostMapping("/generate-image")
    public ResponseEntity<AiImageResponse> generateImage(@RequestBody AiImageRequest request) {
        return ResponseEntity.ok(aiImageService.generateImage(request));
    }
}
