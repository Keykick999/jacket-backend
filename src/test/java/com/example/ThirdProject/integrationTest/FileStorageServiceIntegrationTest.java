package com.example.ThirdProject.integrationTest;

import com.amazonaws.services.s3.AmazonS3;
import com.example.ThirdProject.file.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class FileStorageServiceIntegrationTest {

    @Autowired
    FileStorageService fileStorageService;

    @Autowired
    AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Test
    void storeFile_shouldUploadToMinio() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "hello".getBytes()
        );

        String fileName = fileStorageService.storeFile(file);

        boolean exists = amazonS3.doesObjectExist(bucket, fileName);

        assertThat(exists).isTrue();
    }

    @Test
    void getFileUrl_shouldGeneratePresignedUrl() {
        String fileName = fileStorageService.storeFile(
                "hello".getBytes(),
                "test.png"
        );

        String url = fileStorageService.getFileUrl(fileName);

        assertThat(url).isNotNull();
        assertThat(url).contains("http");
    }

    @Test
    void getFileUrl_shouldDownloadFile() throws IOException {
        // 파일 생성 및 저장
        String fileName = fileStorageService.storeFile(
                "hello".getBytes(),
                "test.png"
        );


        // pre-singedUrl로 파일 다운 가능한지 확인
        String url = fileStorageService.getFileUrl(fileName);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");

        assertThat(conn.getResponseCode()).isEqualTo(200);

    }
}












