package com.example.ThirdProject.ai.service;

import com.example.ThirdProject.ai.dto.AiImageRequest;
import com.example.ThirdProject.ai.dto.AiImageResponse;
import com.example.ThirdProject.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiImageService {

    private final FileStorageService fileStorageService;

    @Value("${huggingface.api.url}")
    private String apiUrl;

    @Value("${huggingface.api.token}")
    private String apiToken;

    public AiImageResponse generateImage(AiImageRequest request) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.IMAGE_PNG));

        Map<String, String> body = new HashMap<>();
        body.put("inputs", request.getPrompt());

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                byte[].class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            // 1. MinIO에 byte[] 저장 (FileStorageService의 수정한 메서드 활용)
            String fileName = fileStorageService.storeFile(response.getBody(), "ai_generated_" + System.currentTimeMillis() + ".png");

            // 2. 고정된 서버 URL 대신 MinIO의 Pre-signed URL을 생성하여 반환
            String fileUrl = fileStorageService.getFileUrl(fileName);

            return AiImageResponse.builder()
                    .fileUrl(fileUrl)
                    .build();
        } else {
            throw new RuntimeException("Failed to generate image from AI service");
        }
    }
}





