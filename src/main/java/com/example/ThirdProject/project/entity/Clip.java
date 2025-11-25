package com.example.ThirdProject.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "clips")
public class Clip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    @JsonIgnore
    private Track track;

    private String name;

    private double startTime;
    private double duration;
    private double offset;

    private String sourceUrl; // URL to the audio file

    private double volume;
    private double pan;
    private boolean isMuted;

    // For future use if we have different clip types (AUDIO, MIDI)
    // private String type; 
}
