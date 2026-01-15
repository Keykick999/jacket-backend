package com.example.ThirdProject.project.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPlugin is a Querydsl query type for Plugin
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPlugin extends EntityPathBase<Plugin> {

    private static final long serialVersionUID = 1635730974L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPlugin plugin = new QPlugin("plugin");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final NumberPath<Integer> orderIndex = createNumber("orderIndex", Integer.class);

    public final ListPath<PluginParameter, QPluginParameter> parameters = this.<PluginParameter, QPluginParameter>createList("parameters", PluginParameter.class, QPluginParameter.class, PathInits.DIRECT2);

    public final QTrack track;

    public final EnumPath<com.example.ThirdProject.project.enums.PluginType> type = createEnum("type", com.example.ThirdProject.project.enums.PluginType.class);

    public QPlugin(String variable) {
        this(Plugin.class, forVariable(variable), INITS);
    }

    public QPlugin(Path<? extends Plugin> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPlugin(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPlugin(PathMetadata metadata, PathInits inits) {
        this(Plugin.class, metadata, inits);
    }

    public QPlugin(Class<? extends Plugin> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.track = inits.isInitialized("track") ? new QTrack(forProperty("track"), inits.get("track")) : null;
    }

}

