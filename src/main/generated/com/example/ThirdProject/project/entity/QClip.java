package com.example.ThirdProject.project.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QClip is a Querydsl query type for Clip
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QClip extends EntityPathBase<Clip> {

    private static final long serialVersionUID = 1672820987L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QClip clip = new QClip("clip");

    public final NumberPath<Double> duration = createNumber("duration", Double.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isMuted = createBoolean("isMuted");

    public final StringPath name = createString("name");

    public final NumberPath<Double> offset = createNumber("offset", Double.class);

    public final NumberPath<Double> pan = createNumber("pan", Double.class);

    public final StringPath sourceUrl = createString("sourceUrl");

    public final NumberPath<Double> startTime = createNumber("startTime", Double.class);

    public final QTrack track;

    public final NumberPath<Double> volume = createNumber("volume", Double.class);

    public QClip(String variable) {
        this(Clip.class, forVariable(variable), INITS);
    }

    public QClip(Path<? extends Clip> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QClip(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QClip(PathMetadata metadata, PathInits inits) {
        this(Clip.class, metadata, inits);
    }

    public QClip(Class<? extends Clip> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.track = inits.isInitialized("track") ? new QTrack(forProperty("track"), inits.get("track")) : null;
    }

}

