package com.example.ThirdProject.project.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.example.ThirdProject.project.dto.QClipDto is a Querydsl Projection type for ClipDto
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QClipDto extends ConstructorExpression<ClipDto> {

    private static final long serialVersionUID = 2137038240L;

    public QClipDto(com.querydsl.core.types.Expression<Long> id, com.querydsl.core.types.Expression<String> name, com.querydsl.core.types.Expression<Double> startTime, com.querydsl.core.types.Expression<Double> duration, com.querydsl.core.types.Expression<Double> offset, com.querydsl.core.types.Expression<String> sourceUrl, com.querydsl.core.types.Expression<Double> volume, com.querydsl.core.types.Expression<Double> pan, com.querydsl.core.types.Expression<Boolean> isMuted) {
        super(ClipDto.class, new Class<?>[]{long.class, String.class, double.class, double.class, double.class, String.class, double.class, double.class, boolean.class}, id, name, startTime, duration, offset, sourceUrl, volume, pan, isMuted);
    }

}

