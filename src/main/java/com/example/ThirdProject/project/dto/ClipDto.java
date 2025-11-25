package com.example.ThirdProject.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClipDto {
    private Long id;
    private String name;
    private double startTime;
    private double duration;
    private double offset;
    private String sourceUrl;
    private double volume;
    private double pan;
    private boolean isMuted;
}
