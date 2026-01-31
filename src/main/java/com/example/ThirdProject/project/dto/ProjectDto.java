package com.example.ThirdProject.project.dto;

import com.example.ThirdProject.project.entity.Project;
import com.example.ThirdProject.project.entity.Track;
import com.querydsl.core.annotations.QueryProjection;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Comparator;
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

    public static ProjectDto from(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .bpm(project.getBpm())
                .timeSignature(project.getTimeSignature())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .albumId(project.getAlbum() != null ? project.getAlbum().getId() : null)
                .tracks(project.getTracks().stream()
                        .filter(track -> !track.isDeleted())
                        .sorted(Comparator.comparingInt(Track::getOrderIndex))
                        .map(TrackDto::from) // TrackDto에도 정적 메서드 구현
                        .toList())
                .build();
    }

    @QueryProjection
    public ProjectDto(Long id, String name, Integer bpm, String timeSignature,
                      LocalDateTime createdAt, LocalDateTime updatedAt, Long albumId,
                      List<TrackDto> tracks) {
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