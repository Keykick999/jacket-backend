package com.example.ThirdProject.file.controller;

import com.example.ThirdProject.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        // 1. MinIO에 파일 저장 및 고유 파일명 반환
        String fileName = fileStorageService.storeFile(file);

        // 2. 외부에서 접근 가능한 Pre-signed URL 생성
        String fileUrl = fileStorageService.getFileUrl(fileName);

        Map<String, String> response = new HashMap<>();
        response.put("fileName", fileName);
        response.put("fileUrl", fileUrl);

        return ResponseEntity.ok(response);
    }
}