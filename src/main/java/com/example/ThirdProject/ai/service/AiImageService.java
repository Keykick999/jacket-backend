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

    @Value("${app.api.url}")
    private String backendApiUrl;

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
            String fileName = fileStorageService.storeFile(response.getBody(), "ai_generated_" + System.currentTimeMillis() + ".png");

            String fileUrl = backendApiUrl + "/uploads/" + fileName;

            return AiImageResponse.builder()
                    .fileUrl(fileUrl)
                    .build();
        } else {
            throw new RuntimeException("Failed to generate image from AI service");
        }
    }
}



