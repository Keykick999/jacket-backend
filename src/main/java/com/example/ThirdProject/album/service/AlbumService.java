package com.example.ThirdProject.album.service;

import com.example.ThirdProject.album.dto.AlbumCreateRequest;
import com.example.ThirdProject.album.dto.AlbumResponse;
import com.example.ThirdProject.album.entity.Album;
import com.example.ThirdProject.album.repository.AlbumRepository;
import com.example.ThirdProject.common.exception.CustomException;
import com.example.ThirdProject.common.exception.ErrorCode;
import com.example.ThirdProject.project.entity.Project;
import com.example.ThirdProject.project.repository.ProjectRepository;
import com.example.ThirdProject.user.entity.User;
import com.example.ThirdProject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public AlbumResponse createAlbum(String userId, AlbumCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Album album = new Album();
        album.setTitle(request.getTitle());
        album.setCoverImageUrl(request.getCoverImageUrl());
        album.setUser(user);

        Album savedAlbum = albumRepository.save(album);
        return AlbumResponse.fromEntity(savedAlbum);
    }

    @Transactional(readOnly = true)
    public List<AlbumResponse> getAlbumsByUserId(String userId) {
        return albumRepository.findByUserId(userId).stream()
                .map(AlbumResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlbumResponse getAlbum(Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));
        return AlbumResponse.fromEntity(album);
    }

    public void addProjectToAlbum(String userId, Long albumId, Long projectId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));
        
        if (!album.getUser().getId().equals(userId)) {
             throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        project.setAlbum(album);
        // projectRepository.save(project); // Transactional handles this
    }
}
