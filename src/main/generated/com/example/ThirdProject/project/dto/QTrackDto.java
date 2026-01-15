package com.example.ThirdProject.project.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.example.ThirdProject.project.dto.QTrackDto is a Querydsl Projection type for TrackDto
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QTrackDto extends ConstructorExpression<TrackDto> {

    private static final long serialVersionUID = -2118154877L;

    public QTrackDto(com.querydsl.core.types.Expression<Long> id, com.querydsl.core.types.Expression<String> name, com.querydsl.core.types.Expression<com.example.ThirdProject.project.enums.TrackType> type, com.querydsl.core.types.Expression<String> color, com.querydsl.core.types.Expression<Float> volume, com.querydsl.core.types.Expression<Float> pan, com.querydsl.core.types.Expression<Boolean> isMuted, com.querydsl.core.types.Expression<Boolean> isSolo, com.querydsl.core.types.Expression<Integer> orderIndex) {
        super(TrackDto.class, new Class<?>[]{long.class, String.class, com.example.ThirdProject.project.enums.TrackType.class, String.class, float.class, float.class, boolean.class, boolean.class, int.class}, id, name, type, color, volume, pan, isMuted, isSolo, orderIndex);
    }

}

