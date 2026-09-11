package com.example.ThirdProject.unitTest;

import com.example.ThirdProject.ai.dto.AiImageRequest;
import com.example.ThirdProject.ai.dto.AiImageResponse;
import com.example.ThirdProject.ai.service.AiImageService;
import com.example.ThirdProject.file.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiImageServiceUnitTest {

    @Mock
    RestTemplate restTemplate;

    @Mock
    FileStorageService fileStorageService;

    @InjectMocks
    AiImageService aiImageService;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                aiImageService,
                "apiUrl",
                "http://test"
        );

        ReflectionTestUtils.setField(
                aiImageService,
                "apiToken",
                "test-token"
        );
    }

    @Test
    void generateImage_success() {
        // given
        AiImageRequest request = new AiImageRequest("cat");

        byte[] fakeImage = "image".getBytes();

        ResponseEntity<byte[]> response =
                new ResponseEntity<>(fakeImage, HttpStatus.OK);

        // when
        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenReturn(response);

        when(fileStorageService.storeFile(any(), any()))
                .thenReturn("ai.png");

        when(fileStorageService.getFileUrl(any()))
                .thenReturn("http://minio/ai.png");

        AiImageResponse result = aiImageService.generateImage(request);

        assertThat(result.getFileUrl()).isEqualTo("http://minio/ai.png");
    }
}