package com.example.ThirdProject.project.entity;

import com.example.ThirdProject.project.enums.PluginType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "plugins")
public class Plugin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private PluginType type; // EQ, COMPRESSOR, etc.

    private Integer orderIndex;

    @OneToMany(mappedBy = "plugin", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PluginParameter> parameters = new ArrayList<>();

    public void addParameter(PluginParameter parameter) {
        parameters.add(parameter);
        parameter.setPlugin(this);
    }

    public void removeParameter(PluginParameter parameter) {
        parameters.remove(parameter);
        parameter.setPlugin(null);
    }
}
