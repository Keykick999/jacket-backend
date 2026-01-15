package com.example.ThirdProject.project.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProject is a Querydsl query type for Project
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProject extends EntityPathBase<Project> {

    private static final long serialVersionUID = -665628274L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProject project = new QProject("project");

    public final com.example.ThirdProject.common.entity.QBaseTimeEntity _super = new com.example.ThirdProject.common.entity.QBaseTimeEntity(this);

    public final com.example.ThirdProject.album.entity.QAlbum album;

    public final NumberPath<Integer> bpm = createNumber("bpm", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final StringPath timeSignature = createString("timeSignature");

    public final ListPath<Track, QTrack> tracks = this.<Track, QTrack>createList("tracks", Track.class, QTrack.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.example.ThirdProject.user.entity.QUser user;

    public QProject(String variable) {
        this(Project.class, forVariable(variable), INITS);
    }

    public QProject(Path<? extends Project> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProject(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProject(PathMetadata metadata, PathInits inits) {
        this(Project.class, metadata, inits);
    }

    public QProject(Class<? extends Project> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.album = inits.isInitialized("album") ? new com.example.ThirdProject.album.entity.QAlbum(forProperty("album"), inits.get("album")) : null;
        this.user = inits.isInitialized("user") ? new com.example.ThirdProject.user.entity.QUser(forProperty("user")) : null;
    }

}

