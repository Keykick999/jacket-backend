package com.example.ThirdProject.common.util;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class S3Utils {

    private final AmazonS3 amazonS3;
    private final String bucketName;

    public S3Utils(AmazonS3 amazonS3, @Value("${cloud.aws.s3.bucket}") String bucketName) {
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
    }

    public String generatePresignedUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isEmpty()) return originalUrl;

        try {
            // 객체 키 추출
            String path = originalUrl.split("\\?")[0];  // 쿼리 스트링 이후 제거
            String objectKey = path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;   // 마지막 슬래시 이후의 파일 경로만 반환

            // 만료 시간 설정 (현재 시간으로부터 2시간)
            Date expiration = Date.from(Instant.now().plus(2, ChronoUnit.HOURS));

            // 요청 생성 및 URL 반환
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectKey)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);

            return amazonS3.generatePresignedUrl(request).toString();
        } catch (Exception e) {
            return originalUrl;
        }
    }
}
