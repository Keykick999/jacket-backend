package com.example.ThirdProject.album.dto;

import com.example.ThirdProject.album.entity.Album;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumDto {
    private Long id;
    private String title;
    private String coverImageUrl;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AlbumDto fromEntity(Album album) {
        return AlbumDto.builder()
                .id(album.getId())
                .title(album.getTitle())
                .coverImageUrl(album.getCoverImageUrl())
                .userId(album.getUser().getId())
                .createdAt(album.getCreatedAt())
                .updatedAt(album.getUpdatedAt())
                .build();
    }
}
