package com.example.ThirdProject.project.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPluginParameter is a Querydsl query type for PluginParameter
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPluginParameter extends EntityPathBase<PluginParameter> {

    private static final long serialVersionUID = 2128318251L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPluginParameter pluginParameter = new QPluginParameter("pluginParameter");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final QPlugin plugin;

    public final StringPath value = createString("value");

    public QPluginParameter(String variable) {
        this(PluginParameter.class, forVariable(variable), INITS);
    }

    public QPluginParameter(Path<? extends PluginParameter> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPluginParameter(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPluginParameter(PathMetadata metadata, PathInits inits) {
        this(PluginParameter.class, metadata, inits);
    }

    public QPluginParameter(Class<? extends PluginParameter> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.plugin = inits.isInitialized("plugin") ? new QPlugin(forProperty("plugin"), inits.get("plugin")) : null;
    }

}

