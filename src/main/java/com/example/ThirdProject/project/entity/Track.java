package com.example.ThirdProject.project.entity;

import com.example.ThirdProject.project.enums.TrackType;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@org.hibernate.annotations.SQLRestriction("deleted = false")
@Table(name = "tracks", indexes = {
        @Index(name = "idx_track_project_deleted_order", columnList = "project_id, deleted, orderIndex")
})
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private TrackType type; // AUDIO, MIDI, etc.

    private String color;

    private double volume = 0.0; // dB

    private double pan = 0.0;

    private boolean isMuted = false;

    private boolean isSolo = false;

    private int orderIndex;

    @Column(nullable = false)
    private boolean deleted = false;

    @Builder.Default
    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Plugin> plugins = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Clip> clips = new ArrayList<>();

    public void addPlugin(Plugin plugin) {
        plugins.add(plugin);
        plugin.setTrack(this);
    }

    public void removePlugin(Plugin plugin) {
        plugins.remove(plugin);
        plugin.setTrack(null);
    }

    public void addClip(Clip clip) {
        clips.add(clip);
        clip.setTrack(this);
    }

    public void removeClip(Clip clip) {
        clips.remove(clip);
        clip.setTrack(null);
    }
}
