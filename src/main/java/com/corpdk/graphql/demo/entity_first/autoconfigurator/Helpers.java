package com.corpdk.graphql.demo.entity_first.autoconfigurator;

import graphql.language.*;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.SelectedField;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Helpers {
    private static final Log logger = LogFactory.getLog(Helpers.class);
    private static final String FILTER = "Filter";
    public static final TypeName INT_FILTER_TYPE = new TypeName("IntFilter");
    public static final TypeName ID_FILTER_TYPE = new TypeName("IDFilter");
    public static final TypeName FLOAT_FILTER_TYPE = new TypeName("FloatFilter");
    public static final TypeName STRING_FILTER_TYPE = new TypeName("StringFilter");
    public static final TypeName BOOLEAN_FILTER_TYPE = new TypeName("BooleanFilter");

    public static final TypeName INT_TYPE = new TypeName("Int");
    public static final TypeName ID_TYPE = new TypeName("ID");
    public static final TypeName FLOAT_TYPE = new TypeName("Float");
    public static final TypeName STRING_TYPE = new TypeName("String");
    public static final TypeName BOOLEAN_TYPE = new TypeName("Boolean");


    private Helpers() {
    }

    public static void addNumericTypeFilter(@NotNull TypeDefinitionRegistry registry, String name) {
        TypeName typeName = new TypeName(name);

        InputObjectTypeDefinition.Builder builder = InputObjectTypeDefinition.newInputObjectDefinition()
                .name(name + FILTER);

        addField(builder, "in", ListType.newListType(typeName).build());
        addField(builder, "nin", ListType.newListType(typeName).build());
        addField(builder,
                List.of("eq", "ne", "lt", "le", "gt", "ge"),
                typeName
        );
        addFilterChain(builder, name);
        registry.add(builder.build());
    }

    public static void addField(@NotNull InputObjectTypeDefinition.Builder builder, String fieldName, Type<?> typeName) {
        builder
                .inputValueDefinition(
                        InputValueDefinition.newInputValueDefinition()
                                .name(fieldName)
                                .type(typeName)
                                .build()
                );
    }

    public static void addField(@NotNull ObjectTypeDefinition.Builder builder, String fieldName, Type<?> typeName) {
        builder
                .fieldDefinition(
                        FieldDefinition.newFieldDefinition()
                                .name(fieldName)
                                .type(typeName)
                                .build()
                );
    }

    public static void addField(InputObjectTypeDefinition.Builder builder, @NotNull List<String> fieldNames, TypeName typeName) {
        fieldNames.forEach(fieldName -> addField(builder, fieldName, typeName));
    }

    public static void addFilterChain(InputObjectTypeDefinition.Builder builder, String name) {
        TypeName typeFilterName = new TypeName(name + FILTER);

        addField(builder,
                List.of("and", "or", "not"),
                typeFilterName
        );
    }

    public static Set<Class<?>> scanForEntities(String basePackage) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        return scanner.findCandidateComponents(basePackage).stream()
                .map(beanDefinition -> {
                    try {
                        return Class.forName(beanDefinition.getBeanClassName());
                    } catch (ClassNotFoundException e) {
                        logger.warn("Failed to load class", e);
                        return null;
                    }
                }).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static boolean isInteger(@NotNull Class<?> fieldType) {
        return fieldType.equals(Long.class) || fieldType.equals(Integer.class);
    }

    public static boolean isDecimal(@NotNull Class<?> fieldType) {
        return fieldType.equals(Double.class) || fieldType.equals(Float.class);
    }

    public static boolean isBasicType(Class<?> fieldType) {
        return isInteger(fieldType) || isDecimal(fieldType) || fieldType.equals(String.class) || fieldType.equals(Boolean.class);
    }

    public static TypeName mapJavaClassToGraphQLFilterType(Class<?> fieldType, @NotNull java.lang.reflect.Field field) {
        if (field.isAnnotationPresent(Id.class)) {
            if (isInteger(fieldType)) {
                return INT_FILTER_TYPE;
            } else if (fieldType.equals(String.class)) {
                return ID_FILTER_TYPE;
            }
        }
        if (isInteger(fieldType)) {
            return INT_FILTER_TYPE;
        } else if (isDecimal(fieldType)) {
            return FLOAT_FILTER_TYPE;
        } else if (fieldType.equals(String.class)) {
            return STRING_FILTER_TYPE;
        } else if (fieldType.equals(Boolean.class)) {
            return BOOLEAN_FILTER_TYPE;
        } else {
            return new TypeName(fieldType.getSimpleName() + FILTER);
        }
    }

    public static TypeName mapJavaClassToGraphQLType(Class<?> fieldType, @NotNull java.lang.reflect.Field field) {
        if (field.isAnnotationPresent(Id.class) && isBasicType(fieldType)) {
            return ID_TYPE;
        }

        if (isInteger(fieldType)) {
            return INT_TYPE;
        } else if (isDecimal(fieldType)) {
            return FLOAT_TYPE;
        } else if (fieldType.equals(String.class)) {
            return STRING_TYPE;
        } else if (fieldType.equals(Boolean.class)) {
            return BOOLEAN_TYPE;
        } else {
            return new TypeName(fieldType.getSimpleName());
        }
    }

    public static boolean requiresProjection(@NotNull TypeInformation<?> domainType, @NotNull Class<?> resultType) {
        return !resultType.equals(domainType.getType());
    }

    public static Collection<String> buildPropertyPaths(DataFetchingFieldSelectionSet selectionSet, @NotNull TypeInformation<?> domainType, Class<?> resultType) {
        if (domainType.getType().equals(resultType) ||
                domainType.getType().isAssignableFrom(resultType) ||
                domainType.isSubTypeOf(resultType)) {
            FieldSelection selection = new FieldSelection(selectionSet);
            Function<String, PropertyPath> pathFactory = path -> PropertyPath.from(path, domainType);
            List<PropertyPath> paths = getPropertyPaths(domainType, selection, pathFactory);
            return paths.stream().map(PropertyPath::toDotPath).toList();
        }
        return Collections.emptyList();
    }

    @NotNull
    private static List<PropertyPath> getPropertyPaths(
            TypeInformation<?> typeInfo, @NotNull FieldSelection selection, Function<String, PropertyPath> pathFactory) {
        List<PropertyPath> result = new ArrayList<>();

        for (SelectedField selectedField : selection) {
            String propertyName = selectedField.getName();
            TypeInformation<?> propertyTypeInfo = typeInfo.getProperty(propertyName);
            if (propertyTypeInfo == null) {
                if (isConnectionEdges(selectedField) || isConnectionEdgeNode(selectedField)) {
                    getConnectionPropertyPaths(typeInfo, selection, pathFactory, selectedField, result);
                }
                continue;
            }

            PropertyPath propertyPath = pathFactory.apply(propertyName);

            List<PropertyPath> nestedPaths = null;
            FieldSelection nestedSelection = selection.select(selectedField);
            if (nestedSelection.isPresent() && propertyTypeInfo.getActualType() != null) {
                TypeInformation<?> actualType = propertyTypeInfo.getRequiredActualType();
                nestedPaths = getPropertyPaths(actualType, nestedSelection, propertyPath::nested);
            }

            result.addAll(CollectionUtils.isEmpty(nestedPaths) ?
                    Collections.singletonList(propertyPath) : nestedPaths);
        }
        return result;
    }

    private static boolean isConnectionEdges(@NotNull SelectedField selectedField) {
        return selectedField.getName().equals("edges") &&
                selectedField.getParentField().getType() instanceof GraphQLNamedOutputType namedType &&
                namedType.getName().endsWith("Connection");
    }

    private static boolean isConnectionEdgeNode(@NotNull SelectedField selectedField) {
        return selectedField.getName().equals("node") && isConnectionEdges(selectedField.getParentField());
    }

    private static void getConnectionPropertyPaths(
            TypeInformation<?> typeInfo, @NotNull FieldSelection selection, Function<String, PropertyPath> pathFactory,
            SelectedField selectedField, List<PropertyPath> result) {

        FieldSelection nestedSelection = selection.select(selectedField);
        if (nestedSelection.isPresent()) {
            TypeInformation<?> actualType = typeInfo.getRequiredActualType();
            List<PropertyPath> paths = getPropertyPaths(actualType, nestedSelection, pathFactory);
            if (!CollectionUtils.isEmpty(paths)) {
                result.addAll(paths);
            }
        }
    }

    static class FieldSelection implements Iterable<SelectedField> {
        private final List<SelectedField> selectedFields;

        private final List<SelectedField> allFields;

        public FieldSelection(@NotNull DataFetchingFieldSelectionSet selectionSet) {
            this.selectedFields = selectionSet.getImmediateFields();
            this.allFields = selectionSet.getFields();
        }

        private FieldSelection(List<SelectedField> selectedFields, List<SelectedField> allFields) {
            this.selectedFields = selectedFields;
            this.allFields = allFields;
        }

        public boolean isPresent() {
            return !this.selectedFields.isEmpty();
        }

        public FieldSelection select(SelectedField field) {
            List<SelectedField> finalSelectedFields = new ArrayList<>();

            for (SelectedField selectedField : this.allFields) {
                if (field.equals(selectedField.getParentField())) {
                    finalSelectedFields.add(selectedField);
                }
            }

            return new FieldSelection(finalSelectedFields, this.allFields);
        }

        @Override
        @NotNull
        public Iterator<SelectedField> iterator() {
            return this.selectedFields.iterator();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> Path<T> getPath(Root root, String pathString) {
        String[] segments = pathString.split("\\.");
        Path<?> path = root;

        for (String segment : segments) {
            path = path.get(segment);
        }

        return (Path<T>) path;
    }
}
