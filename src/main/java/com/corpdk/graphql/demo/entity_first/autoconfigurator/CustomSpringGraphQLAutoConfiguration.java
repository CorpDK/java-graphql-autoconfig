package com.corpdk.graphql.demo.entity_first.autoconfigurator;

import com.corpdk.graphql.demo.entity_first.autoconfigurator.generators.GraphQLFilterTypesConfigurer;
import com.corpdk.graphql.demo.entity_first.autoconfigurator.generators.GraphQLEntityTypesConfigurer;
import graphql.execution.instrumentation.Instrumentation;
import graphql.introspection.Introspection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.log.LogMessage;
import org.springframework.graphql.execution.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
public class CustomSpringGraphQLAutoConfiguration {
    private static final Log logger = LogFactory.getLog(CustomSpringGraphQLAutoConfiguration.class);

    @Value("${application.entity.package:}")
    private String entityBasePackage;

    public CustomSpringGraphQLAutoConfiguration() {
        logger.info("Using CustomSpringGraphQLAutoConfiguration");
    }

    @Bean
    public GraphQlSource graphQlSource(ResourcePatternResolver resourcePatternResolver, GraphQlProperties properties,
                                       ObjectProvider<DataFetcherExceptionResolver> exceptionResolvers,
                                       ObjectProvider<SubscriptionExceptionResolver> subscriptionExceptionResolvers,
                                       ObjectProvider<Instrumentation> instrumentations, ObjectProvider<RuntimeWiringConfigurer> wiringConfigurers,
                                       ObjectProvider<GraphQlSourceBuilderCustomizer> sourceCustomizers) {
        logger.info("My GraphQL Source Bean");
        String[] schemaLocations = properties.getSchema().getLocations();
        Resource[] schemaResources = resolveSchemaResources(resourcePatternResolver, schemaLocations,
                properties.getSchema().getFileExtensions());
        GraphQlSource.SchemaResourceBuilder builder = (new GraphQLSourceBuilder())
                .schemaResources(schemaResources)
                .exceptionResolvers(exceptionResolvers.orderedStream().toList())
                .subscriptionExceptionResolvers(subscriptionExceptionResolvers.orderedStream().toList())
                .instrumentation(instrumentations.orderedStream().toList());
        if (properties.getSchema().getInspection().isEnabled()) {
            builder.inspectSchemaMappings(logger::info);
        }
        if (!properties.getSchema().getIntrospection().isEnabled()) {
            Introspection.enabledJvmWide(false);
        }
        builder.configureTypeDefinitions(new GraphQLEntityTypesConfigurer(entityBasePackage));
        builder.configureTypeDefinitions(new GraphQLFilterTypesConfigurer(entityBasePackage));
        builder.configureTypeDefinitions(new ConnectionTypeDefinitionConfigurer());
        wiringConfigurers.orderedStream().forEach(builder::configureRuntimeWiring);
        sourceCustomizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    private Resource[] resolveSchemaResources(ResourcePatternResolver resolver, String[] locations,
                                              String[] extensions) {
        List<Resource> resources = new ArrayList<>();
        for (String location : locations) {
            for (String extension : extensions) {
                resources.addAll(resolveSchemaResources(resolver, location + "*" + extension));
            }
        }
        return resources.toArray(new Resource[0]);
    }

    private List<Resource> resolveSchemaResources(ResourcePatternResolver resolver, String pattern) {
        try {
            return Arrays.asList(resolver.getResources(pattern));
        } catch (IOException ex) {
            logger.debug(LogMessage.format("Could not resolve schema location: '%s'", pattern), ex);
            return Collections.emptyList();
        }
    }

    // TODO: 4) Generate GraphQL Query For All the GraphQL Types Generated in Step 2
    // <Lowercase<Entity>>s(filter: <Entity>Filter, first:Int, after:String, last:Int, before:String): <Entity>Connection
    // count<Entity>(filter: <Entity>Filter): Int
    // list<Entity>s(filter: <Entity>Filter): [<Entity>]
    // <Lowercase<Entity>>(id: ID, unique: <Entity>UniqueInput): <Entity>
    // TODO: 6) Create Generic Filter resolver / DataFetcher
    // TODO: 7) Registers the resolvers / DataFetcher
}
