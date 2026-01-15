package com.example.ThirdProject.project.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.example.ThirdProject.project.dto.QProjectDto is a Querydsl Projection type for ProjectDto
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QProjectDto extends ConstructorExpression<ProjectDto> {

    private static final long serialVersionUID = 1457631541L;

    public QProjectDto(com.querydsl.core.types.Expression<Long> id, com.querydsl.core.types.Expression<String> name, com.querydsl.core.types.Expression<Integer> bpm, com.querydsl.core.types.Expression<String> timeSignature, com.querydsl.core.types.Expression<java.time.LocalDateTime> createdAt, com.querydsl.core.types.Expression<java.time.LocalDateTime> updatedAt, com.querydsl.core.types.Expression<Long> albumId, com.querydsl.core.types.Expression<? extends java.util.List<TrackDto>> tracks) {
        super(ProjectDto.class, new Class<?>[]{long.class, String.class, int.class, String.class, java.time.LocalDateTime.class, java.time.LocalDateTime.class, long.class, java.util.List.class}, id, name, bpm, timeSignature, createdAt, updatedAt, albumId, tracks);
    }

}

