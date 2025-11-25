package com.example.ThirdProject.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "plugin_parameters")
public class PluginParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plugin_id", nullable = false)
    private Plugin plugin;

    @Column(nullable = false)
    private String name; // Parameter key/name

    @Column(nullable = false)
    private String value; // Parameter value
}
