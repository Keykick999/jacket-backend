package com.example.ThirdProject.album.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAlbum is a Querydsl query type for Album
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAlbum extends EntityPathBase<Album> {

    private static final long serialVersionUID = 130762062L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAlbum album = new QAlbum("album");

    public final com.example.ThirdProject.common.entity.QBaseTimeEntity _super = new com.example.ThirdProject.common.entity.QBaseTimeEntity(this);

    public final StringPath coverImageUrl = createString("coverImageUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<com.example.ThirdProject.project.entity.Project, com.example.ThirdProject.project.entity.QProject> projects = this.<com.example.ThirdProject.project.entity.Project, com.example.ThirdProject.project.entity.QProject>createList("projects", com.example.ThirdProject.project.entity.Project.class, com.example.ThirdProject.project.entity.QProject.class, PathInits.DIRECT2);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.example.ThirdProject.user.entity.QUser user;

    public QAlbum(String variable) {
        this(Album.class, forVariable(variable), INITS);
    }

    public QAlbum(Path<? extends Album> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAlbum(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAlbum(PathMetadata metadata, PathInits inits) {
        this(Album.class, metadata, inits);
    }

    public QAlbum(Class<? extends Album> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.example.ThirdProject.user.entity.QUser(forProperty("user")) : null;
    }

}

