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

        // 1. 페이징 처리: 최소한의 프로젝션으로 ID와 기본 정보만 조회
        // (필요 시 검색 조건인 where절을 여기에 추가)
        List<ProjectDto> content = queryFactory
                .select(Projections.fields(ProjectDto.class,
                        project.id,
                        project.name,
                        project.createdAt
                ))
                .from(project)
                .where(project.user.id.eq(userId))
                .orderBy(project.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1) // 다음 페이지 확인용 +1
                .fetch();

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }

        if (content.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), pageable, hasNext);
        }

        // 2. 배치 조회: 조회된 프로젝트들의 트랙 정보를 IN 절로 한 번에 가져옴
        List<Long> projectIds = content.stream().map(ProjectDto::getId).toList();

        Map<Long, List<TrackDto>> trackMap = queryFactory
                .from(track)
                .where(track.project.id.in(projectIds))
                .transform(groupBy(track.project.id).as(list(Projections.fields(TrackDto.class,
                        track.id,
                        track.name,
                        track.type,
                        track.orderIndex
                ))));

        // 3. 메모리 조립: 가져온 트랙 리스트를 각 프로젝트 DTO에 매핑
        content.forEach(p -> p.setTracks(trackMap.getOrDefault(p.getId(), Collections.emptyList())));

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