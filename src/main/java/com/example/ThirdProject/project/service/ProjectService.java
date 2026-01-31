package com.example.ThirdProject.project.service;

import com.example.ThirdProject.common.exception.CustomException;
import com.example.ThirdProject.common.exception.ErrorCode;
import com.example.ThirdProject.common.util.S3Utils;
import com.example.ThirdProject.project.dto.ProjectDto;
import com.example.ThirdProject.project.entity.*;
import com.example.ThirdProject.project.repository.ProjectQueryRepository;
import com.example.ThirdProject.project.repository.ProjectRepository;
import com.example.ThirdProject.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectQueryRepository projectQueryRepository;
    private final S3Utils s3Utils;

    // 프로젝트 생성
    public ProjectDto createProject(User user, ProjectDto projectDto) {
        Project project = Project.builder()
                .user(user)
                .name(projectDto.getName())
                .bpm(projectDto.getBpm())
                .timeSignature(projectDto.getTimeSignature())
                .build();

        Optional.ofNullable(projectDto.getTracks()).orElse(List.of())
                .forEach(trackDto -> project.addTrack(trackDto.toEntity(project)));

        return ProjectDto.from(projectRepository.save(project));
    }

    // 프로젝트 목록 조회
    @Transactional(readOnly = true)
    public Slice<ProjectDto> getProjectsByUserId(String userId, Pageable pageable) {
        return projectQueryRepository.searchProjects(userId, pageable);
    }

    // 특정 프로젝트 조회
    @Transactional(readOnly = true)
    public ProjectDto getProject(Long projectId, String userId) {
        ProjectDto projectDto = projectQueryRepository.getProjectDetail(projectId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        // S3 SDK를 사용하여 URL 갱신
        projectDto.getTracks().forEach(track -> {
            track.getClips().forEach(clip -> {
                clip.setSourceUrl(s3Utils.generatePresignedUrl(clip.getSourceUrl()));
            });
        });

        return projectDto;
    }

    // 프로젝트 수정
    public ProjectDto updateProject(Long projectId, String userId, ProjectDto projectDto) {
        Project project = findAndValidateProject(projectId, userId); // 공통 검증 로직 분리 가능

        // 엔티티 필드 수정
        project.updateInfo(projectDto);

        // 트랙 교체
        List<Track> newTracks = Optional.ofNullable(projectDto.getTracks()).orElse(List.of())
                .stream()
                .map(trackDto -> trackDto.toEntity(project))
                .toList();
        project.replaceTracks(newTracks);

        return ProjectDto.from(projectRepository.save(project));
    }

    // 프로젝트 삭제
    public void deleteProject(String userId, Long projectId) {
        Project project = findAndValidateProject(projectId, userId); // 공통 검증 로직 분리 가능

        projectRepository.delete(project);
    }

    // 앨범에 수록된 프로젝트들 조회
    @Transactional(readOnly = true)
    public Slice<ProjectDto> getProjectsByAlbumId(Long albumId, Pageable pageable) {
        return projectQueryRepository.searchProjectsByAlbumId(albumId, pageable);
    }

    private Project findAndValidateProject(Long projectId, String userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        return project;
    }
}
