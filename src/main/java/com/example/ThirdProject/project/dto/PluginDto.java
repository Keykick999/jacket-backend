package com.example.ThirdProject.project.dto;

import com.example.ThirdProject.project.entity.Plugin;
import com.example.ThirdProject.project.entity.PluginParameter;
import com.example.ThirdProject.project.enums.PluginType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@Builder
public class PluginDto {
    private Long id;
    private String name;
    private PluginType type;
    private Map<String, String> parameters;
    private Integer orderIndex;

    public Plugin toEntity() {
        Plugin plugin = Plugin.builder()
                .name(this.name)
                .type(this.type)
                .orderIndex(this.orderIndex)
                .build();

        Optional.ofNullable(this.parameters)
                .orElse(Collections.emptyMap())
                .forEach((key, value) -> plugin.addParameter(
                        PluginParameter.builder().name(key).value(value).build()
                ));

        return plugin;
    }

    public static PluginDto from(Plugin plugin) {
        return PluginDto.builder()
                .id(plugin.getId())
                .name(plugin.getName())
                .type(plugin.getType())
                .orderIndex(plugin.getOrderIndex())
                .parameters(plugin.getParameters().stream()
                        .collect(Collectors.toMap(PluginParameter::getName, PluginParameter::getValue)))
                .build();
    }
}
