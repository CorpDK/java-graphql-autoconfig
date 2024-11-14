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

public class GraphQLOperationsConfigurer implements TypeDefinitionConfigurer {
    private static final Log logger = LogFactory.getLog(GraphQLOperationsConfigurer.class);

    private final String entityBasePackage;

    public GraphQLOperationsConfigurer(String entityBasePackage) {
        this.entityBasePackage = entityBasePackage;
    }

    @Override
    public void configure(@NotNull TypeDefinitionRegistry registry) {
        addEntityQueries(registry);
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

        ObjectTypeDefinition updatedQuery = queryType.transform(builder -> fieldDefinitions.forEach(builder::fieldDefinition));

        logger.debug("Updated Query Type: " + updatedQuery);

        registry.remove(queryType);
        registry.add(updatedQuery);
    }
}
