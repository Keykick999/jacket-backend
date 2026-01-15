package com.example.ThirdProject.project.controller;

import com.example.ThirdProject.project.dto.ProjectDto;
import com.example.ThirdProject.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Slice<ProjectDto>> getProjects(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        String userId = userDetails.getUsername();
        return ResponseEntity.ok(projectService.getProjectsByUserId(userId, pageable));
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
        projectService.deleteProject(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{projectId}/tracks/{trackId}")
    public ResponseEntity<Void> deleteTrack(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long projectId, @PathVariable Long trackId) {
        String userId = userDetails.getUsername();
        projectService.deleteTrack(projectId, trackId, userId);
        return ResponseEntity.noContent().build();
    }
}
