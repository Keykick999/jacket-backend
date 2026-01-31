package com.example.ThirdProject.track.service;

import com.example.ThirdProject.common.exception.CustomException;
import com.example.ThirdProject.common.exception.ErrorCode;
import com.example.ThirdProject.project.entity.Project;
import com.example.ThirdProject.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackService {

    private final ProjectRepository projectRepository;

    // 트랙 삭제
    public void deleteTrack(Long projectId, Long trackId, String userId) {
        Project project = findAndValidateProject(projectId, userId); // 공통 검증 로직 분리 가능
        project.softDeleteTrack(trackId);
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
