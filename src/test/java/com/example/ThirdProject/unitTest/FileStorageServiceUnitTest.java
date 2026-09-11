package com.example.ThirdProject.unitTest;

import com.amazonaws.services.s3.AmazonS3;
import com.example.ThirdProject.file.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceUnitTest {

    @Mock
    AmazonS3 amazonS3;

    @InjectMocks
    FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                fileStorageService,
                "bucket",
                "test-bucket"
        );
    }

    @Test
    void storeFile_shouldCallS3PutObject() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "hello".getBytes()
        );

        String fileName = fileStorageService.storeFile(file);

        verify(amazonS3).putObject(
                eq("test-bucket"),
                anyString(),
                any(),
                any()
        );

        assertThat(fileName).contains("test.png");
    }

    @Test
    void getFileUrl_shouldCallGeneratePresignedUrl() throws MalformedURLException {
        when(amazonS3.generatePresignedUrl(any()))
                .thenReturn(new URL("http://test-url"));

        String url = fileStorageService.getFileUrl("file.png");

        assertThat(url).isEqualTo("http://test-url");
    }
}