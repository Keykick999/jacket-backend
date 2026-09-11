package com.example.ThirdProject.album.controller;

import com.example.ThirdProject.album.dto.AlbumCreateRequest;
import com.example.ThirdProject.album.dto.AlbumResponse;
import com.example.ThirdProject.album.service.AlbumService;
import com.example.ThirdProject.common.entity.CustomUserDetails;
import com.example.ThirdProject.project.dto.ProjectDto;
import com.example.ThirdProject.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;
    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<AlbumResponse> createAlbum(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody AlbumCreateRequest request) {
        return ResponseEntity.ok(albumService.createAlbum(userDetails.user(), request));
    }

    @GetMapping
    public ResponseEntity<List<AlbumResponse>> getMyAlbums(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        return ResponseEntity.ok(albumService.getAlbumsByUserId(userId));
    }

    @GetMapping("/{albumId}")
    public ResponseEntity<AlbumResponse> getAlbum(@PathVariable Long albumId) {
        return ResponseEntity.ok(albumService.getAlbum(albumId));
    }

    @PostMapping("/{albumId}/projects/{projectId}")
    public ResponseEntity<Void> addProjectToAlbum(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long albumId,
            @PathVariable Long projectId) {
        String userId = userDetails.getUsername();
        albumService.addProjectToAlbum(userId, albumId, projectId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{albumId}/projects")
    public ResponseEntity<Slice<ProjectDto>> getProjectsByAlbum(@PathVariable Long albumId, Pageable pageable) {
        return ResponseEntity.ok(projectService.getProjectsByAlbumId(albumId, pageable));
    }
}
