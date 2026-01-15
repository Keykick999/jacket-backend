package com.example.ThirdProject.project.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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

    @QueryProjection
    public ClipDto(Long id, String name, double startTime, double duration, double offset,
                   String sourceUrl, double volume, double pan, boolean isMuted) {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.duration = duration;
        this.offset = offset;
        this.sourceUrl = sourceUrl;
        this.volume = volume;
        this.pan = pan;
        this.isMuted = isMuted;
    }
}
