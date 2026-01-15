package com.example.ThirdProject.project.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@Builder
@Setter
public class ProjectDto {
    private Long id;
    private String name;
    private Integer bpm;
    private String timeSignature;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long albumId;
    private List<TrackDto> tracks;

    @QueryProjection
    public ProjectDto(Long id, String name, Integer bpm, String timeSignature,
                      LocalDateTime createdAt, LocalDateTime updatedAt, Long albumId,
                      List<TrackDto> tracks) { // 생성자에 tracks 추가 (총 8개 인자)
        this.id = id;
        this.name = name;
        this.bpm = bpm;
        this.timeSignature = timeSignature;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.albumId = albumId;
        this.tracks = tracks;
    }
}