package com.example.ThirdProject.project.dto;

import com.example.ThirdProject.project.enums.PluginType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class PluginDto {
    private Long id;
    private String name;
    private PluginType type;
    private Map<String, String> parameters;
    private Integer orderIndex;
}
