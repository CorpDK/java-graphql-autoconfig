package com.corpdk.graphql.demo.entity_first.autoconfigurator;

import graphql.language.FieldDefinition;
import graphql.schema.*;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.WiringFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Configuration
public class ConfigureFetchers implements RuntimeWiringConfigurer {
    private static final Log logger = LogFactory.getLog(ConfigureFetchers.class);

    private final Map<String, DataFetcherFactory> dataFetcherFactories;

    public ConfigureFetchers(@NotNull Map<String, DataFetcherFactory> factories) {
        logger.info("My Fetcher Configuration: " + factories.keySet().size());
        this.dataFetcherFactories = factories;
    }

    @Override
    public void setTypeDefinitionRegistry(@NotNull TypeDefinitionRegistry registry) {
        RuntimeWiringConfigurer.super.setTypeDefinitionRegistry(registry);
    }

    @Override
    public void configure(@NotNull RuntimeWiring.Builder builder) {
        // This Method is not needed
    }

    @Override
    public void configure(@NotNull RuntimeWiring.Builder builder, @NotNull List<WiringFactory> container) {
        container.add(new AutoRegistrationWiringFactory(builder));
    }

    @SuppressWarnings({"rawtypes"})
    public interface DataFetcherFactory {
        DataFetcher single();

        DataFetcher many();

        DataFetcher scrollable();

        DataFetcher count();
    }

    private class AutoRegistrationWiringFactory implements WiringFactory {

        private final RuntimeWiring.Builder builder;

        @Nullable
        private Predicate<String> existingQueryDataFetcherPredicate;

        AutoRegistrationWiringFactory(RuntimeWiring.Builder builder) {
            this.builder = builder;
        }

        @Override
        public boolean providesDataFetcher(FieldWiringEnvironment environment) {
            if (ConfigureFetchers.this.dataFetcherFactories.isEmpty()) {
                return false;
            }

            if (!environment.getParentType().getName().equals("Query")) {
                return false;
            }

            String outputTypeName = getOutputTypeName(environment);
            logger.debug("Output Type: " + outputTypeName);

            boolean result = (outputTypeName != null &&
                    ConfigureFetchers.this.dataFetcherFactories.containsKey(outputTypeName) &&
                    !hasDataFetcherFor(environment.getFieldDefinition()));

            if (!result) {
                // This may be called multiples times on success, so log only rejections from here
                logTraceMessage(environment, outputTypeName, false);
            }

            return result;
        }

        @Nullable
        private String getOutputTypeName(@NotNull FieldWiringEnvironment environment) {
            GraphQLType outputType = removeNonNullWrapper(environment.getFieldType());

            if (isConnectionType(outputType)) {
                String name = ((GraphQLObjectType) outputType).getName();
                return name.substring(0, name.length() - 10);
            }

            if (outputType instanceof GraphQLList graphQLList) {
                outputType = removeNonNullWrapper((graphQLList).getWrappedType());
            }

            if (outputType instanceof GraphQLNamedOutputType namedType) {
                String outputTypeName = namedType.getName();
                String fieldName = environment.getFieldDefinition().getName();
                if (outputTypeName.equals("Int") && fieldName.startsWith("count")) {
                    String parentTypeName = fieldName.substring(5, fieldName.length() - 1);
                    logger.debug("Is Count Field of: " + parentTypeName);
                    return parentTypeName;
                } else {
                    return outputTypeName;
                }
            }

            return null;
        }

        private GraphQLType removeNonNullWrapper(GraphQLType outputType) {
            return (outputType instanceof GraphQLNonNull wrapper) ? wrapper.getWrappedType() : outputType;
        }

        private boolean isConnectionType(GraphQLType type) {
            return (type instanceof GraphQLObjectType objectType &&
                    objectType.getName().endsWith("Connection") &&
                    objectType.getField("edges") != null && objectType.getField("pageInfo") != null);
        }

        private boolean hasDataFetcherFor(FieldDefinition fieldDefinition) {
            if (this.existingQueryDataFetcherPredicate == null) {
                Map<String, ?> map = this.builder.build().getDataFetchersForType("Query");
                this.existingQueryDataFetcherPredicate = fieldName -> map.get(fieldName) != null;
            }
            return this.existingQueryDataFetcherPredicate.test(fieldDefinition.getName());
        }

        private void logTraceMessage(FieldWiringEnvironment environment, @Nullable String typeName, boolean match) {
            if (logger.isTraceEnabled()) {
                String query = environment.getFieldDefinition().getName();
                logger.trace((match ? "Matched" : "Skipped") +
                        " output typeName " + ((typeName != null) ? "'" + typeName + "'" : "null") +
                        " for query '" + query + "'");
            }
        }

        @Override
        public DataFetcher<?> getDataFetcher(FieldWiringEnvironment environment) {
            String outputTypeName = getOutputTypeName(environment);

            DataFetcherFactory factory = ConfigureFetchers.this.dataFetcherFactories.get(outputTypeName);
            Assert.notNull(factory, "Expected DataFetcher factory for typeName '" + outputTypeName + "'");
            logger.debug("Query Name: " + environment.getFieldDefinition().getName());

            GraphQLType type = removeNonNullWrapper(environment.getFieldType());
            if (isConnectionType(type)) {
                logTraceMessage(environment, outputTypeName, true);
                return factory.scrollable();
            } else if (environment.getFieldDefinition().getName().startsWith("count")) {
                logTraceMessage(environment, "Int", true);
                return factory.count();
            } else if (type instanceof GraphQLList) {
                logTraceMessage(environment, outputTypeName, true);
                return factory.many();
            } else {
                logTraceMessage(environment, outputTypeName, true);
                return factory.single();
            }
        }
    }
}
