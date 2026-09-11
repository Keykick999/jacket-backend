package com.example.ThirdProject.project.entity;

import com.example.ThirdProject.album.entity.Album;
import com.example.ThirdProject.common.entity.BaseTimeEntity;
import com.example.ThirdProject.common.exception.CustomException;
import com.example.ThirdProject.common.exception.ErrorCode;
import com.example.ThirdProject.project.dto.ProjectDto;
import com.example.ThirdProject.user.entity.User;
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
@Table(name = "projects", indexes = {
        @Index(name = "idx_project_user_created", columnList = "user_id, createdAt DESC")
})
public class Project extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    @Column(nullable = false)
    private String name;

    private Integer bpm = 120;

    private String timeSignature = "4/4";

    @Builder.Default
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Track> tracks = new ArrayList<>();

    public void addTrack(Track track) {
        tracks.add(track);
        track.setProject(this);
    }

    public void updateInfo(ProjectDto dto) {
        this.name = dto.getName();
        this.bpm = dto.getBpm();
        this.timeSignature = dto.getTimeSignature();
    }

    public void replaceTracks(List<Track> newTracks) {
        this.tracks.clear();
        newTracks.forEach(this::addTrack);
    }

    public void softDeleteTrack(Long trackId) {
        Track track = this.tracks.stream()
                .filter(t -> t.getId().equals(trackId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.TRACK_NOT_FOUND));
        track.setDeleted(true);
    }
}
