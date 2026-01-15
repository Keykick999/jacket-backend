package com.example.ThirdProject.project.dto;

import com.example.ThirdProject.project.enums.TrackType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class TrackDto {
    private Long id;
    private String name;
    private TrackType type;
    private String color;
    private Float volume;
    private Float pan;
    private Boolean isMuted;
    private Boolean isSolo;
    private Integer orderIndex;
    private List<PluginDto> plugins;
    private List<ClipDto> clips;

    @QueryProjection
    public TrackDto(Long id, String name, TrackType type, String color, Float volume,
                    Float pan, Boolean isMuted, Boolean isSolo, Integer orderIndex) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.color = color;
        this.volume = volume;
        this.pan = pan;
        this.isMuted = isMuted;
        this.isSolo = isSolo;
        this.orderIndex = orderIndex;
    }
}
