package com.example.ThirdProject.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserProvider is a Querydsl query type for UserProvider
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserProvider extends EntityPathBase<UserProvider> {

    private static final long serialVersionUID = 1693864943L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserProvider userProvider = new QUserProvider("userProvider");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath provider = createString("provider");

    public final StringPath providerId = createString("providerId");

    public final QUser user;

    public QUserProvider(String variable) {
        this(UserProvider.class, forVariable(variable), INITS);
    }

    public QUserProvider(Path<? extends UserProvider> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserProvider(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserProvider(PathMetadata metadata, PathInits inits) {
        this(UserProvider.class, metadata, inits);
    }

    public QUserProvider(Class<? extends UserProvider> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}

