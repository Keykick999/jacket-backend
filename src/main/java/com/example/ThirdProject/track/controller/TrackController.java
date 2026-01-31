package com.example.ThirdProject.track.controller;

import com.example.ThirdProject.track.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api/tracks")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    @DeleteMapping("/{projectId}/{trackId}")
    public ResponseEntity<Void> deleteTrack(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long projectId, @PathVariable Long trackId) {
        String userId = userDetails.getUsername();
        trackService.deleteTrack(projectId, trackId, userId);
        return ResponseEntity.noContent().build();
    }
}
