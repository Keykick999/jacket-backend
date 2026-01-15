package com.example.ThirdProject.project.repository;

import com.example.ThirdProject.project.dto.*;
import com.example.ThirdProject.project.entity.QClip;
import com.example.ThirdProject.project.entity.QPlugin;
import com.example.ThirdProject.project.entity.QProject;
import com.example.ThirdProject.project.entity.QTrack;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
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
                        project.album.id,
                        Expressions.asSimple(new ArrayList<>())
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
        // 1. 프로젝트 기본 정보 조회 (QProjectDto 인자 7개 생성자 사용)
        ProjectDto projectDto = queryFactory
                .select(new QProjectDto(
                        project.id,
                        project.name,
                        project.bpm,
                        project.timeSignature,
                        project.createdAt,
                        project.updatedAt,
                        project.album.id,
                        Expressions.asSimple(new ArrayList<>())
                ))
                .from(project)
                .where(project.id.eq(projectId).and(project.user.id.eq(userId)))
                .fetchOne();

        // 프로젝트가 없거나 권한이 없으면 즉시 종료
        if (projectDto == null) return null;

        // 2. 해당 프로젝트에 속한 트랙 목록 조회 (QTrackDto 인자 9개 생성자 사용)
        List<TrackDto> tracks = queryFactory
                .select(new QTrackDto(
                        track.id,
                        track.name,
                        track.type,
                        track.color,
                        track.volume.floatValue(), // DoublePath를 Float으로 변환 (에러 방지)
                        track.pan.floatValue(),    // DoublePath를 Float으로 변환 (에러 방지)
                        track.isMuted,
                        track.isSolo,
                        track.orderIndex
                ))
                .from(track)
                .where(track.project.id.eq(projectId))
                .orderBy(track.orderIndex.asc())
                .fetch();

        projectDto.setTracks(tracks);

        // 3. 트랙들에 속한 클립들을 한 번에 조회하여 매핑 (N+1 방지)
        List<Long> trackIds = tracks.stream()
                .map(TrackDto::getId)
                .toList();

        if (!trackIds.isEmpty()) {
            // 클립 정보와 소속 트랙 ID를 함께 가져오기 위해 Projections.constructor 사용
            // 별도의 DTO 클래스 없이 Map으로 조립하기 위한 전략
            var clipsWithTrackId = queryFactory
                    .select(Projections.fields(ClipGroupDto.class,
                            clip.track.id.as("trackId"),
                            new QClipDto(
                                    clip.id,
                                    clip.name,
                                    clip.startTime,
                                    clip.duration,
                                    clip.offset,
                                    clip.sourceUrl,
                                    clip.volume,
                                    clip.pan,
                                    clip.isMuted
                            ).as("clipDto")
                    ))
                    .from(clip)
                    .where(clip.track.id.in(trackIds))
                    .fetch();

            // 트랙 ID별로 클립 리스트 그룹화
            Map<Long, List<ClipDto>> clipsMap = clipsWithTrackId.stream()
                    .collect(Collectors.groupingBy(
                            ClipGroupDto::getTrackId,
                            Collectors.mapping(ClipGroupDto::getClipDto, Collectors.toList())
                    ));

            // 각 트랙 객체에 조회된 클립 리스트 주입
            tracks.forEach(t ->
                    t.setClips(clipsMap.getOrDefault(t.getId(), Collections.emptyList()))
            );
        }

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