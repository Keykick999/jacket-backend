package com.example.ThirdProject.project.controller;

import com.example.ThirdProject.project.service.BulkDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/test")
@RequiredArgsConstructor
public class AdminTestDataController {

    private final BulkDataService bulkDataService;

    /**
     * 1. 유저만 먼저 생성
     * POST /api/admin/test/users?count=100
     */
    @PostMapping("/users")
    public ResponseEntity<String> createUsers(@RequestParam(defaultValue = "100") int count) {
        bulkDataService.createBulkUsers(count);
        return ResponseEntity.ok(count + "명의 테스트 유저가 생성되었습니다.");
    }

    /**
     * 2. 생성된 유저들에게 프로젝트 데이터 세팅
     * POST /api/admin/test/projects
     */
    @PostMapping("/projects")
    public ResponseEntity<String> createProjectData() {
        bulkDataService.createProjectDataForTestUsers();
        return ResponseEntity.ok("테스트 유저들을 위한 프로젝트 데이터 생성이 완료되었습니다.");
    }
}