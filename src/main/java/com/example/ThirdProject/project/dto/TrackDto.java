package com.example.ThirdProject.project.dto;

import com.example.ThirdProject.project.enums.TrackType;
import lombok.Data;
import java.util.List;

@Data
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
}
