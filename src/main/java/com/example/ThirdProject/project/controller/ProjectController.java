package com.example.ThirdProject.project.controller;

import com.example.ThirdProject.project.dto.ProjectDto;
import com.example.ThirdProject.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(@AuthenticationPrincipal UserDetails userDetails, @RequestBody ProjectDto projectDto) {
        String userId = userDetails.getUsername();
        return ResponseEntity.ok(projectService.createProject(userId, projectDto));
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getProjects(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        return ResponseEntity.ok(projectService.getProjectsByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProject(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        String userId = userDetails.getUsername();
        return ResponseEntity.ok(projectService.getProject(id, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> updateProject(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id, @RequestBody ProjectDto projectDto) {
        String userId = userDetails.getUsername();
        return ResponseEntity.ok(projectService.updateProject(id, userId, projectDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        String userId = userDetails.getUsername();
        projectService.deleteProject(id, userId);
        return ResponseEntity.noContent().build();
    }
}
