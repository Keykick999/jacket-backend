package com.example.ThirdProject.project.dto;

import com.example.ThirdProject.project.entity.Clip;
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

    public static ClipDto from(Clip clip) {
        return ClipDto.builder()
                .id(clip.getId())
                .name(clip.getName())
                .startTime(clip.getStartTime())
                .duration(clip.getDuration())
                .offset(clip.getOffset())
                .sourceUrl(clip.getSourceUrl())
                .volume(clip.getVolume())
                .pan(clip.getPan())
                .isMuted(clip.isMuted())
                .build();
    }

    public Clip toEntity() {
        return Clip.builder()
                .name(this.name)
                .startTime(this.startTime)
                .duration(this.duration)
                .offset(this.offset)
                .sourceUrl(this.sourceUrl)
                .volume(this.volume)
                .pan(this.pan)
                .isMuted(this.isMuted)
                .build();
    }

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
