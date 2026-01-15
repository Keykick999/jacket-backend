package com.example.ThirdProject.project.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTrack is a Querydsl query type for Track
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTrack extends EntityPathBase<Track> {

    private static final long serialVersionUID = 333713664L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTrack track = new QTrack("track");

    public final ListPath<Clip, QClip> clips = this.<Clip, QClip>createList("clips", Clip.class, QClip.class, PathInits.DIRECT2);

    public final StringPath color = createString("color");

    public final BooleanPath deleted = createBoolean("deleted");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isMuted = createBoolean("isMuted");

    public final BooleanPath isSolo = createBoolean("isSolo");

    public final StringPath name = createString("name");

    public final NumberPath<Integer> orderIndex = createNumber("orderIndex", Integer.class);

    public final NumberPath<Double> pan = createNumber("pan", Double.class);

    public final ListPath<Plugin, QPlugin> plugins = this.<Plugin, QPlugin>createList("plugins", Plugin.class, QPlugin.class, PathInits.DIRECT2);

    public final QProject project;

    public final EnumPath<com.example.ThirdProject.project.enums.TrackType> type = createEnum("type", com.example.ThirdProject.project.enums.TrackType.class);

    public final NumberPath<Double> volume = createNumber("volume", Double.class);

    public QTrack(String variable) {
        this(Track.class, forVariable(variable), INITS);
    }

    public QTrack(Path<? extends Track> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTrack(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTrack(PathMetadata metadata, PathInits inits) {
        this(Track.class, metadata, inits);
    }

    public QTrack(Class<? extends Track> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.project = inits.isInitialized("project") ? new QProject(forProperty("project"), inits.get("project")) : null;
    }

}

