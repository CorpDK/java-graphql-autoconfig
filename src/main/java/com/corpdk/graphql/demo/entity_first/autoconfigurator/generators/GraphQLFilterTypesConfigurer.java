package com.corpdk.graphql.demo.entity_first.autoconfigurator.generators;

import graphql.language.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.graphql.execution.TypeDefinitionConfigurer;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

import static com.corpdk.graphql.demo.entity_first.autoconfigurator.Helpers.*;

public class GraphQLFilterTypesConfigurer implements TypeDefinitionConfigurer {
    private static final Log logger = LogFactory.getLog(GraphQLFilterTypesConfigurer.class);

    private final String entityBasePackage;

    public GraphQLFilterTypesConfigurer(String entityBasePackage) {
        this.entityBasePackage = entityBasePackage;
    }

    @Override
    public void configure(@NotNull TypeDefinitionRegistry registry) {
        addDefaultFilterTypes(registry);

        addEntityFilterTypes(registry);

        addEntityQueries(registry);
    }

    private void addEntityFilterTypes(@NotNull TypeDefinitionRegistry registry) {
        logger.info("Generating GraphQL Type Filter Schema for all Entities");
        Set<Class<?>> entityClasses = scanForEntities(entityBasePackage);

        logger.debug("entityClasses: " + entityClasses.stream().map(Class::getSimpleName).collect(Collectors.joining("; ")));

        entityClasses.forEach(aClass -> {
            logger.debug("Class: " + aClass.getSimpleName());
            InputObjectTypeDefinition.Builder builder = InputObjectTypeDefinition.newInputObjectDefinition();
            builder.name(aClass.getSimpleName() + "Filter");

            Arrays.stream(aClass.getDeclaredFields()).forEach(field -> {
                Class<?> fieldType = field.getType();
                String name = field.getName();
                logger.debug("\tField: " + name + "; Type: " + fieldType.getSimpleName());

                TypeName typeName;

                if (Collection.class.isAssignableFrom(fieldType)) {
                    ParameterizedType listType = (ParameterizedType) field.getGenericType();
                    Class<?> relatedEntityClass = (Class<?>) listType.getActualTypeArguments()[0];
                    logger.debug("\t\tSub Type: " + relatedEntityClass.getSimpleName());
                    typeName = mapJavaClassToGraphQLFilterType(relatedEntityClass, field);
                } else {
                    typeName = mapJavaClassToGraphQLFilterType(fieldType, field);
                }

                addField(builder, name, typeName);
            });

            addFilterChain(builder, aClass.getSimpleName());

            InputObjectTypeDefinition definition = builder.build();
            registry.add(definition);
        });
    }

    private void addDefaultFilterTypes(@NotNull TypeDefinitionRegistry registry) {
        logger.info("Adding GraphQL Base Type Filter Schema");

        InputObjectTypeDefinition.Builder stringFilterInputBuilder = InputObjectTypeDefinition.newInputObjectDefinition()
                .name("StringFilter");
        addField(stringFilterInputBuilder, "in", ListType.newListType(STRING_TYPE).build());
        addField(stringFilterInputBuilder, "nin", ListType.newListType(STRING_TYPE).build());
        addField(stringFilterInputBuilder,
                List.of("eq", "ne", "like", "contains", "startsWith", "endsWith"),
                STRING_TYPE
        );
        addField(stringFilterInputBuilder, "len", INT_TYPE);
        addField(stringFilterInputBuilder, "sensitive", BOOLEAN_TYPE);
        addFilterChain(stringFilterInputBuilder, "String");
        InputObjectTypeDefinition stringFilterInput = stringFilterInputBuilder.build();
        registry.add(stringFilterInput);

        InputObjectTypeDefinition.Builder idFilterInputBuilder = InputObjectTypeDefinition.newInputObjectDefinition()
                .name("IDFilter");
        addField(idFilterInputBuilder, "in", ListType.newListType(ID_TYPE).build());
        addField(idFilterInputBuilder, "nin", ListType.newListType(ID_TYPE).build());
        addField(idFilterInputBuilder,
                List.of("eq", "ne"),
                ID_TYPE
        );
        addFilterChain(idFilterInputBuilder, "ID");
        InputObjectTypeDefinition idFilterInput = idFilterInputBuilder.build();
        registry.add(idFilterInput);

        InputObjectTypeDefinition.Builder booleanFilterBuilder = InputObjectTypeDefinition.newInputObjectDefinition()
                .name("BooleanFilter");
        addField(booleanFilterBuilder, "is", BOOLEAN_TYPE);
        InputObjectTypeDefinition booleanFilterInput = booleanFilterBuilder.build();
        registry.add(booleanFilterInput);

        addNumericTypeFilter(registry, "Int");
        addNumericTypeFilter(registry, "Float");
    }

    private void addEntityQueries(@NotNull TypeDefinitionRegistry registry) {
        logger.info("Generating GraphQL Query Type Schema for all Entities");

        Set<Class<?>> entityClasses = scanForEntities(entityBasePackage);

        logger.debug("entityClasses: " + entityClasses.stream().map(Class::getSimpleName).collect(Collectors.joining("; ")));

        Optional<?> optionalQueryType = registry.getType("Query");
        ObjectTypeDefinition queryType;
        queryType = optionalQueryType
                .map(ObjectTypeDefinition.class::cast)
                .orElseGet(() ->
                        ObjectTypeDefinition.newObjectTypeDefinition()
                                .name("Query")
                                .build());

        logger.debug("Query Type: " + queryType);

        List<FieldDefinition> fieldDefinitions = new ArrayList<>();

        entityClasses.forEach(aClass -> {
            logger.debug("Class: " + aClass.getSimpleName());
            String name = aClass.getSimpleName();

            List<InputValueDefinition> inputValueDefinitions = new ArrayList<>();

            InputValueDefinition filterInput = InputValueDefinition.newInputValueDefinition()
                    .name("filter")
                    .type(new TypeName(name + "Filter"))
                    .build();

            InputValueDefinition first = InputValueDefinition.newInputValueDefinition().name("first").type(INT_TYPE).build();
            InputValueDefinition after = InputValueDefinition.newInputValueDefinition().name("after").type(STRING_TYPE).build();
            InputValueDefinition last = InputValueDefinition.newInputValueDefinition().name("last").type(INT_TYPE).build();
            InputValueDefinition before = InputValueDefinition.newInputValueDefinition().name("before").type(STRING_TYPE).build();

            inputValueDefinitions.add(filterInput);
            inputValueDefinitions.add(first);
            inputValueDefinitions.add(after);
            inputValueDefinitions.add(last);
            inputValueDefinitions.add(before);

            FieldDefinition.Builder builderPaged = FieldDefinition.newFieldDefinition()
                    .name(name.toLowerCase() + "s")
                    .inputValueDefinitions(inputValueDefinitions)
                    .type(new TypeName(name + "Connection"));

            fieldDefinitions.add(builderPaged.build());

            FieldDefinition.Builder builderCount = FieldDefinition.newFieldDefinition()
                    .name("count" + name + "s")
                    .inputValueDefinition(filterInput)
                    .type(INT_TYPE);

            fieldDefinitions.add(builderCount.build());

            FieldDefinition.Builder builderList = FieldDefinition.newFieldDefinition()
                    .name("list" + name + "s")
                    .inputValueDefinitions(inputValueDefinitions)
                    .type(ListType.newListType(new TypeName(name)).build());

            fieldDefinitions.add(builderList.build());
        });

        ObjectTypeDefinition updatedQuery = queryType.transform(builder -> {
            builder
                    .fieldDefinition(
                            FieldDefinition.newFieldDefinition()
                                    .name("test")
                                    .type(STRING_TYPE)
                                    .build()
                    );
            fieldDefinitions.forEach(builder::fieldDefinition);
        });

        logger.debug("Updated Query Type: " + updatedQuery);

        registry.remove(queryType);
        registry.add(updatedQuery);
    }
}
