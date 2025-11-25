package com.example.ThirdProject.project.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectDto {
    private Long id;
    private String name;
    private Integer bpm;
    private String timeSignature;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TrackDto> tracks;
}
