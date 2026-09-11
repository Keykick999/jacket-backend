package com.example.ThirdProject.integrationTest;

import com.example.ThirdProject.ai.dto.AiImageRequest;
import com.example.ThirdProject.ai.dto.AiImageResponse;
import com.example.ThirdProject.ai.service.AiImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class AiImageServiceIntegrationTest  {

    @Autowired
    private AiImageService aiImageService;

    @Test
    void generateImage_success() {
        // given
        AiImageRequest request = new AiImageRequest("새의 이미지를 만들어주세요.");

        // when
        AiImageResponse response = aiImageService.generateImage(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileUrl()).isNotNull();
    }
}














