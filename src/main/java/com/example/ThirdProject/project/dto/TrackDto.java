package com.example.ThirdProject.project.dto;

import com.example.ThirdProject.project.entity.Plugin;
import com.example.ThirdProject.project.entity.Project;
import com.example.ThirdProject.project.entity.Track;
import com.example.ThirdProject.project.enums.TrackType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    public Track toEntity(Project project) {
        Track track = Track.builder()
                .project(project)
                .name(this.name)
                .type(this.type)
                .color(this.color)
                .volume(this.volume)
                .pan(this.pan)
                .isMuted(this.isMuted)
                .isSolo(this.isSolo)
                .orderIndex(this.orderIndex)
                .build();

        Optional.ofNullable(this.plugins).orElse(List.of())
                .forEach(dto -> track.addPlugin(dto.toEntity()));

        Optional.ofNullable(this.clips).orElse(List.of())
                .forEach(dto -> track.addClip(dto.toEntity())); // ClipDto에도 toEntity 구현 필요

        return track;
    }

    // TrackDto.java 내부
    public static TrackDto from(Track track) {
        return TrackDto.builder()
                .id(track.getId())
                .name(track.getName())
                .type(track.getType())
                .color(track.getColor())
                .volume((float) track.getVolume())
                .pan((float) track.getPan())
                .isMuted(track.isMuted())
                .isSolo(track.isSolo())
                .orderIndex(track.getOrderIndex())
                .plugins(track.getPlugins().stream()
                        .sorted(Comparator.comparingInt(Plugin::getOrderIndex))
                        .map(PluginDto::from)
                        .toList())
                .clips(track.getClips().stream()
                        .map(ClipDto::from)
                        .toList())
                .build();
    }

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
