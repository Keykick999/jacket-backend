package com.example.ThirdProject.file.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * MultipartFile 업로드
     */
    public String storeFile(MultipartFile file) {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            amazonS3.putObject(bucket, fileName, file.getInputStream(), metadata);
            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("MinIO 저장 중 오류 발생: " + fileName, ex);
        }
    }

    /**
     * byte[] 데이터 업로드 (API 결과 저장 등)
     */
    public String storeFile(byte[] content, String originalFileName) {
        String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.length);

            amazonS3.putObject(bucket, fileName, new ByteArrayInputStream(content), metadata);
            return fileName;
        } catch (Exception ex) {
            throw new RuntimeException("MinIO 저장 중 오류 발생: " + fileName, ex);
        }
    }

    /**
     * 파일 조회를 위한 Pre-signed URL 생성
     * 유효기간을 설정하여 보안성을 높이고 서버 부하를 줄임
     */
    public String getFileUrl(String fileName) {
        if (fileName == null) return null;

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 60; // 1시간 유효
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucket, fileName)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);

        return amazonS3.generatePresignedUrl(generatePresignedUrlRequest).toString();
    }
}