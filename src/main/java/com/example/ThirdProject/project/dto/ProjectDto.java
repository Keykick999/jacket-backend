package com.example.ThirdProject.project.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDto {
    private Long id;
    private String name;
    private Integer bpm;
    private String timeSignature;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long albumId;
    private List<TrackDto> tracks;

    @QueryProjection // 이 어노테이션이 반드시 있어야 Q파일이 생성됩니다!
    public ProjectDto(Long id, String name, Integer bpm, String timeSignature,
                      LocalDateTime createdAt, LocalDateTime updatedAt, Long albumId) {
        this.id = id;
        this.name = name;
        this.bpm = bpm;
        this.timeSignature = timeSignature;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.albumId = albumId;
    }
}