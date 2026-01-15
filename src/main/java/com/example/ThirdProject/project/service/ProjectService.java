package com.example.ThirdProject.project.service;

import com.example.ThirdProject.common.exception.CustomException;
import com.example.ThirdProject.common.exception.ErrorCode;
import com.example.ThirdProject.project.dto.ClipDto;
import com.example.ThirdProject.project.dto.PluginDto;
import com.example.ThirdProject.project.dto.ProjectDto;
import com.example.ThirdProject.project.dto.TrackDto;
import com.example.ThirdProject.project.entity.*;
import com.example.ThirdProject.project.repository.ProjectQueryRepository;
import com.example.ThirdProject.project.repository.ProjectRepository;
import com.example.ThirdProject.user.entity.User;
import com.example.ThirdProject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectQueryRepository projectQueryRepository;

    public ProjectDto createProject(String userId, ProjectDto projectDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Project project = new Project();
        project.setUser(user);
        project.setName(projectDto.getName());
        project.setBpm(projectDto.getBpm());
        project.setTimeSignature(projectDto.getTimeSignature());

        // Map Tracks and Plugins
        if (projectDto.getTracks() != null) {
            for (TrackDto trackDto : projectDto.getTracks()) {
                project.addTrack(mapToEntity(trackDto, project));
            }
        }

        Project savedProject = projectRepository.save(project);
        return mapToDto(savedProject);
    }

    @Transactional(readOnly = true)
    public Slice<ProjectDto> getProjectsByUserId(String userId, Pageable pageable) {
        return projectQueryRepository.searchProjects(userId, pageable);
    }

    @Transactional(readOnly = true)
    public ProjectDto getProject(Long projectId, String userId) {
        ProjectDto projectDto = projectQueryRepository.getProjectDetail(projectId, userId);

        // 결과가 null이면 해당 프로젝트가 없거나 본인 소유가 아님
        if (projectDto == null) {
            throw new CustomException(ErrorCode.PROJECT_NOT_FOUND);
        }

        return projectDto;
    }

    public ProjectDto updateProject(Long projectId, String userId, ProjectDto projectDto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        project.setName(projectDto.getName());
        project.setBpm(projectDto.getBpm());
        project.setTimeSignature(projectDto.getTimeSignature());

        // Simple update strategy: Clear existing tracks and re-add
        project.getTracks().clear();

        if (projectDto.getTracks() != null) {
            for (TrackDto trackDto : projectDto.getTracks()) {
                project.addTrack(mapToEntity(trackDto, project));
            }
        }

        return mapToDto(projectRepository.save(project));
    }

    public void deleteProject(String userId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        projectRepository.delete(project);
    }

    public void deleteTrack(Long projectId, Long trackId, String userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        Track trackToRemove = project.getTracks().stream()
                .filter(t -> t.getId().equals(trackId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.TRACK_NOT_FOUND));

        trackToRemove.setDeleted(true);
    }

    @Transactional(readOnly = true)
    public Slice<ProjectDto> getProjectsByAlbumId(Long albumId, Pageable pageable) {
        return projectQueryRepository.searchProjectsByAlbumId(albumId, pageable);
    }

    private Track mapToEntity(TrackDto dto, Project project) {
        Track track = new Track();
        track.setProject(project);
        track.setName(dto.getName());
        track.setType(dto.getType());
        track.setColor(dto.getColor());
        track.setVolume(dto.getVolume());
        track.setPan(dto.getPan());
        track.setMuted(dto.getIsMuted());
        track.setSolo(dto.getIsSolo());
        track.setOrderIndex(dto.getOrderIndex());

        if (dto.getPlugins() != null) {
            for (PluginDto pluginDto : dto.getPlugins()) {
                Plugin plugin = new Plugin();
                plugin.setName(pluginDto.getName());
                plugin.setType(pluginDto.getType());
                plugin.setOrderIndex(pluginDto.getOrderIndex());

                if (pluginDto.getParameters() != null) {
                    for (Map.Entry<String, String> entry : pluginDto.getParameters().entrySet()) {
                        PluginParameter param = new PluginParameter();
                        param.setName(entry.getKey());
                        param.setValue(entry.getValue());
                        plugin.addParameter(param);
                    }
                }

                track.addPlugin(plugin);
            }
        }

        if (dto.getClips() != null) {
            for (ClipDto clipDto : dto.getClips()) {
                Clip clip = new Clip();
                clip.setName(clipDto.getName());
                clip.setStartTime(clipDto.getStartTime());
                clip.setDuration(clipDto.getDuration());
                clip.setOffset(clipDto.getOffset());
                clip.setSourceUrl(clipDto.getSourceUrl());
                clip.setVolume(clipDto.getVolume());
                clip.setPan(clipDto.getPan());
                clip.setMuted(clipDto.isMuted());

                track.addClip(clip);
            }
        }

        return track;
    }

    private ProjectDto mapToDto(Project project) {
        // ProjectDto.builder()를 사용하여 객체를 생성합니다.
        // 만약 DTO에 @Builder가 없다면 setter를 사용하되
        // ProjectDto에 @NoArgsConstructor가 반드시 있어야 합니다.

        List<TrackDto> trackDtos = project.getTracks().stream()
                .filter(track -> !track.isDeleted()) // 삭제된 트랙 제외 로직 추가 권장
                .sorted(Comparator.comparingInt(Track::getOrderIndex))
                .map(this::mapTrackToDto)
                .collect(Collectors.toList());

        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .bpm(project.getBpm())
                .timeSignature(project.getTimeSignature())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .albumId(project.getAlbum() != null ? project.getAlbum().getId() : null)
                .tracks(trackDtos)
                .build();
    }

    // 트랙 변환 로직 분리 (가독성)
    private TrackDto mapTrackToDto(Track track) {
        return TrackDto.builder()
                .id(track.getId())
                .name(track.getName())
                .type(track.getType())
                .color(track.getColor())
                .volume((float) track.getVolume())
                .pan((float) track.getPan())
                .isMuted(track.isMuted())
                .isSolo(track.isSolo())
                .orderIndex(track.getOrderIndex())
                .plugins(track.getPlugins().stream()
                        .sorted(Comparator.comparingInt(Plugin::getOrderIndex))
                        .map(this::mapPluginToDto)
                        .collect(Collectors.toList()))
                .clips(track.getClips().stream()
                        .map(this::mapClipToDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private PluginDto mapPluginToDto(Plugin plugin) {
        return PluginDto.builder()
                .id(plugin.getId())
                .name(plugin.getName())
                .type(plugin.getType())
                .orderIndex(plugin.getOrderIndex())
                .parameters(plugin.getParameters().stream()
                        .collect(Collectors.toMap(PluginParameter::getName, PluginParameter::getValue)))
                .build();
    }

    private ClipDto mapClipToDto(Clip clip) {
        return ClipDto.builder()
                .id(clip.getId())
                .name(clip.getName())
                .startTime(clip.getStartTime())
                .duration(clip.getDuration())
                .offset(clip.getOffset())
                .sourceUrl(clip.getSourceUrl())
                .volume(clip.getVolume())
                .pan(clip.getPan())
                .isMuted(clip.isMuted())
                .build();
    }
}
