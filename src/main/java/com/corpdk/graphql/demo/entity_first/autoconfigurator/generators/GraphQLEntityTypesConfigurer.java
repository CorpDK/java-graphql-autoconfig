package com.corpdk.graphql.demo.entity_first.autoconfigurator.generators;

import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.Type;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.graphql.execution.TypeDefinitionConfigurer;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static com.corpdk.graphql.demo.entity_first.autoconfigurator.Helpers.*;

public class GraphQLEntityTypesConfigurer implements TypeDefinitionConfigurer {
    private static final Log logger = LogFactory.getLog(GraphQLEntityTypesConfigurer.class);

    private final String entityBasePackage;

    public GraphQLEntityTypesConfigurer(String entityBasePackage) {
        this.entityBasePackage = entityBasePackage;
    }

    @Override
    public void configure(TypeDefinitionRegistry registry) {
        logger.info("Generating GraphQL Type Schema for all Entities");
        Set<Class<?>> entityClasses = scanForEntities(entityBasePackage);

        logger.debug("entityClasses: " + entityClasses.stream().map(Class::getSimpleName).collect(Collectors.joining("; ")));

        entityClasses.forEach(entity -> {
            logger.debug("Class: " + entity.getSimpleName());
            ObjectTypeDefinition definition = generateObjectType(entity);
            logger.debug("TypeDefinition: " + definition);
            registry.add(definition);
        });
    }

    private ObjectTypeDefinition generateObjectType(Class<?> entity) {
        ObjectTypeDefinition.Builder builder = ObjectTypeDefinition.newObjectTypeDefinition();
        builder.name(entity.getSimpleName());

        Arrays.stream(entity.getDeclaredFields()).forEach(field -> {
            Class<?> fieldType = field.getType();
            String name = field.getName();
            logger.debug("\tField: " + name + "; Type: " + fieldType.getSimpleName());

            Type<?> typeName;

            if (Collection.class.isAssignableFrom(fieldType)) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                Class<?> relatedEntityClass = (Class<?>) listType.getActualTypeArguments()[0];
                logger.debug("\t\tSub Type: " + relatedEntityClass.getSimpleName());
                typeName = ListType.newListType(mapJavaClassToGraphQLType(relatedEntityClass, field)).build();
            } else {
                typeName = mapJavaClassToGraphQLType(fieldType, field);
            }

            if (field.isAnnotationPresent(Column.class) && !field.getAnnotation(Column.class).nullable()) {
                addField(builder, name, NonNullType.newNonNullType(typeName).build());
            } else if (field.isAnnotationPresent(JoinColumn.class) && !field.getAnnotation(JoinColumn.class).nullable()) {
                addField(builder, name, NonNullType.newNonNullType(typeName).build());
            } else if (field.isAnnotationPresent(Id.class)) {
                addField(builder, name, NonNullType.newNonNullType(typeName).build());
            } else {
                addField(builder, name, typeName);
            }
        });

        return builder.build();
    }
}
