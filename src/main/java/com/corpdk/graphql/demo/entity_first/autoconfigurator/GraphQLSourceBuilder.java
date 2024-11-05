package com.corpdk.graphql.demo.entity_first.autoconfigurator;

import graphql.GraphQL;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.UnionTypeDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import graphql.schema.idl.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.graphql.execution.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class GraphQLSourceBuilder extends AbstractGraphQlSourceBuilder<GraphQlSource.SchemaResourceBuilder>
        implements GraphQlSource.SchemaResourceBuilder {

    private static final Log logger = LogFactory.getLog(GraphQLSourceBuilder.class);

    private final Set<Resource> schemaResources = new LinkedHashSet<>();

    private final List<TypeDefinitionConfigurer> typeDefinitionConfigurers = new ArrayList<>();

    private final List<RuntimeWiringConfigurer> runtimeWiringConfigurers = new ArrayList<>();

    @Nullable
    private TypeResolver typeResolver;

    @Nullable
    private BiFunction<TypeDefinitionRegistry, RuntimeWiring, GraphQLSchema> schemaFactory;

    @Nullable
    private Consumer<SchemaReport> schemaReportConsumer;

    private Consumer<SchemaMappingInspector.Initializer> inspectorInitializerConsumer = initializer -> {
    };

    @Nullable
    private Consumer<GraphQLSchema> schemaReportRunner;

    @SuppressWarnings("rawtypes")
    private static void updateForCustomRootOperationTypeNames(
            TypeDefinitionRegistry registry, RuntimeWiring runtimeWiring) {

        Optional<SchemaDefinition> schemaDefinition = registry.schemaDefinition();

        if (schemaDefinition.isEmpty()) {
            return;
        }

        schemaDefinition.get().getOperationTypeDefinitions().forEach(definition -> {
            String name = StringUtils.capitalize(definition.getName());
            Map<String, DataFetcher> dataFetcherMap = runtimeWiring.getDataFetchers().remove(name);
            if (!CollectionUtils.isEmpty(dataFetcherMap)) {
                runtimeWiring.getDataFetchers().put(definition.getTypeName().getName(), dataFetcherMap);
            }
        });
    }

    @Override
    @NotNull
    protected GraphQLSchema initGraphQlSchema() {
        TypeDefinitionRegistry registry = this.schemaResources.stream()
                .map(this::parse)
                .reduce(TypeDefinitionRegistry::merge)
                .orElse(new TypeDefinitionRegistry());

        for (TypeDefinitionConfigurer configurer : this.typeDefinitionConfigurers) {
            configurer.configure(registry);
        }

        if (logger.isDebugEnabled()) {
            String resources = this.schemaResources.stream()
                    .map(Resource::getDescription)
                    .collect(Collectors.joining(","));
            logger.debug("Loaded GraphQL schema resources: (" + resources + ")");
        }

        RuntimeWiring runtimeWiring = initRuntimeWiring(registry);
        updateForCustomRootOperationTypeNames(registry, runtimeWiring);

        TypeResolver newTypeResolver = initTypeResolver();
        registry.types().values().forEach(def -> {
            if (def instanceof UnionTypeDefinition || def instanceof InterfaceTypeDefinition) {
                runtimeWiring.getTypeResolvers().putIfAbsent(def.getName(), newTypeResolver);
            }
        });

        // SchemaMappingInspector needs RuntimeWiring, but cannot run here since type
        // visitors may transform the schema, for example to add Connection types.

        if (this.schemaReportConsumer != null) {
            this.schemaReportRunner = schema ->
                    this.schemaReportConsumer.accept(createSchemaReport(schema, runtimeWiring));
        }

        return (this.schemaFactory != null) ?
                this.schemaFactory.apply(registry, runtimeWiring) :
                new SchemaGenerator().makeExecutableSchema(registry, runtimeWiring);
    }

    private RuntimeWiring initRuntimeWiring(TypeDefinitionRegistry typeRegistry) {
        RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();
        this.runtimeWiringConfigurers.forEach(configurer -> {
            configurer.setTypeDefinitionRegistry(typeRegistry);
            configurer.configure(builder);
        });

        List<WiringFactory> factories = new ArrayList<>();
        WiringFactory factory = builder.build().getWiringFactory();
        if (!factory.getClass().equals(NoopWiringFactory.class)) {
            factories.add(factory);
        }
        this.runtimeWiringConfigurers.forEach(configurer -> configurer.configure(builder, factories));
        if (!factories.isEmpty()) {
            builder.wiringFactory(new CombinedWiringFactory(factories));
        }

        return builder.build();
    }

    private TypeResolver initTypeResolver() {
        return (this.typeResolver != null) ? this.typeResolver : new ClassNameTypeResolver();
    }

    @Override
    @NotNull
    public GraphQlSource.SchemaResourceBuilder schemaResources(Resource... resources) {
        this.schemaResources.addAll(Arrays.asList(resources));
        return this;
    }

    @Override
    @NotNull
    public GraphQlSource.SchemaResourceBuilder configureTypeDefinitions(@NotNull TypeDefinitionConfigurer configurer) {
        this.typeDefinitionConfigurers.add(configurer);
        return this;
    }

    @Override
    @NotNull
    public GraphQlSource.SchemaResourceBuilder configureRuntimeWiring(@NotNull RuntimeWiringConfigurer configurer) {
        this.runtimeWiringConfigurers.add(configurer);
        return this;
    }

    @Override
    @NotNull
    public GraphQlSource.SchemaResourceBuilder defaultTypeResolver(@NotNull TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
        return this;
    }

    @Override
    @NotNull
    public GraphQlSource.SchemaResourceBuilder inspectSchemaMappings(@NotNull Consumer<SchemaReport> reportConsumer) {
        this.schemaReportConsumer = reportConsumer;
        return this;
    }

    @Override
    @NotNull
    public GraphQlSource.SchemaResourceBuilder inspectSchemaMappings(Consumer<SchemaMappingInspector.Initializer> initializerConsumer, @NotNull Consumer<SchemaReport> reportConsumer) {
        this.inspectorInitializerConsumer = initializerConsumer.andThen(initializerConsumer);
        this.schemaReportConsumer = reportConsumer;
        return this;
    }

    @Override
    @NotNull
    public GraphQlSource.SchemaResourceBuilder schemaFactory(@NotNull BiFunction<TypeDefinitionRegistry, RuntimeWiring, GraphQLSchema> schemaFactory) {
        this.schemaFactory = schemaFactory;
        return this;
    }

    private SchemaReport createSchemaReport(GraphQLSchema schema, RuntimeWiring runtimeWiring) {
        SchemaMappingInspector.Initializer initializer = SchemaMappingInspector.initializer();

        // Add explicit mappings from ClassNameTypeResolver's
        runtimeWiring.getTypeResolvers().values().stream().distinct().forEach(resolver -> {
            if (resolver instanceof ClassNameTypeResolver classNameTypeResolver) {
                classNameTypeResolver.getMappings().forEach((aClass, name) -> initializer.classMapping(name, aClass));
            }
        });

        this.inspectorInitializerConsumer.accept(initializer);

        return initializer.inspect(schema, runtimeWiring.getDataFetchers());
    }

    private TypeDefinitionRegistry parse(Resource schemaResource) {
        Assert.notNull(schemaResource, "'schemaResource' not provided");
        Assert.isTrue(schemaResource.exists(), "'schemaResource' must exist: " + schemaResource);
        try {
            try (InputStream inputStream = schemaResource.getInputStream()) {
                return new SchemaParser().parse(inputStream);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to load schema resource: " + schemaResource);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse schema resource: " + schemaResource, ex);
        }
    }

    @Override
    protected void applyGraphQlConfigurers(@NotNull GraphQL.Builder builder) {
        super.applyGraphQlConfigurers(builder);
        if (this.schemaReportRunner != null) {
            GraphQLSchema schema = builder.build().getGraphQLSchema();
            this.schemaReportRunner.accept(schema);
        }
    }
}
