package com.example.ThirdProject.project.repository;

import com.example.ThirdProject.project.dto.*;
import com.example.ThirdProject.project.entity.QClip;
import com.example.ThirdProject.project.entity.QPlugin;
import com.example.ThirdProject.project.entity.QProject;
import com.example.ThirdProject.project.entity.QTrack;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.ThirdProject.project.entity.QClip.clip;
import static com.example.ThirdProject.project.entity.QPlugin.plugin;
import static com.example.ThirdProject.project.entity.QProject.project;
import static com.example.ThirdProject.project.entity.QTrack.track;
import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;

@Repository
@RequiredArgsConstructor
public class ProjectQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 프로젝트 목록 조회 (Slice 기반 무한 스크롤)
     */
    public Slice<ProjectDto> searchProjects(String userId, Pageable pageable) {
        // 1. 기본 프로젝트 정보와 트랙 정보를 Join해서 가져옵니다.
        List<ProjectDto> content = queryFactory
                .select(new QProjectDto(
                        project.id,
                        project.name,
                        project.bpm,
                        project.timeSignature,
                        project.createdAt,
                        project.updatedAt,
                        project.album.id
                ))
                .from(project)
                .where(project.user.id.eq(userId))
                .orderBy(project.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1) // 다음 페이지 확인용 +1
                .fetch();

        // 2. 만약 상세 정보(트랙 등)가 더 필요하다면, 여기서 추가 쿼리 없이
        // fetchJoin을 사용하거나, 위 쿼리를 유지한 채 반환합니다.
        // (현재 로그상으로는 transform 단계에서 터지므로 fetch()로 바꾸는 것만으로도 해결됩니다.)

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    /**
     * 단일 프로젝트 상세 조회 (모든 계층 데이터 포함)
     */
    public ProjectDto getProjectDetail(Long projectId, String userId) {
        // 상세 조회의 경우, 데이터가 깊으므로(Project > Track > Clip)
        // 쿼리를 쪼개서 가져오는 것이 사용자님이 제시하신 패턴과 일치하며 효율적입니다.

        // 1. Project & Tracks (1번 쿼리)
        ProjectDto projectDto = queryFactory
                .from(project)
                .leftJoin(project.tracks, track)
                .where(project.id.eq(projectId).and(project.user.id.eq(userId)))
                .transform(groupBy(project.id).as(Projections.fields(ProjectDto.class,
                        project.id, project.name, project.bpm, project.timeSignature,
                        list(Projections.fields(TrackDto.class,
                                track.id, track.name, track.type, track.orderIndex)).as("tracks")
                ))).get(projectId);

        if (projectDto == null) return null;

        List<Long> trackIds = projectDto.getTracks().stream().map(TrackDto::getId).toList();
        if (trackIds.isEmpty()) return projectDto;

        // 2. Clips & Plugins (IN 절을 이용한 쿼리 분해)
        Map<Long, List<ClipDto>> clipsMap = queryFactory
                .from(clip)
                .where(clip.track.id.in(trackIds))
                .transform(groupBy(clip.track.id).as(list(Projections.fields(ClipDto.class,
                        clip.id, clip.name, clip.sourceUrl, clip.startTime, clip.duration))));

        // 3. 조립
        projectDto.getTracks().forEach(t -> t.setClips(clipsMap.getOrDefault(t.getId(), Collections.emptyList())));

        return projectDto;
    }

    /**
     * 앨범 ID 기준 프로젝트 목록 조회 (Slice)
     */
    public Slice<ProjectDto> searchProjectsByAlbumId(Long albumId, Pageable pageable) {
        List<ProjectDto> content = queryFactory
                .select(Projections.fields(ProjectDto.class,
                        project.id,
                        project.name,
                        project.createdAt
                ))
                .from(project)
                .where(project.album.id.eq(albumId)) // 앨범 ID 조건
                .orderBy(project.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }
}