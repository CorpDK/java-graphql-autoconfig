package com.corpdk.graphql.demo.entity_first.autoconfigurator.specifications;

import com.corpdk.graphql.demo.entity_first.autoconfigurator.filters.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static graphql.util.StringKit.capitalize;

public class SpecificationGenerator {
    private static final Log logger = LogFactory.getLog(SpecificationGenerator.class);

    private SpecificationGenerator() {
    }

    public static <E> Specification<E> buildSpecification(BaseFilter<?> filter) throws IllegalAccessException {
        return buildSpecification(filter, null);
    }

    public static <E> Specification<E> buildSpecification(BaseFilter<?> filter, String parentPath) throws IllegalAccessException {
        if (filter == null) {
            return null;
        }
        logger.info("Building Specification for: " + filter.getClass().getSimpleName());
        Specification<E> resultSpec = Specification.where(null); // Base specification

        for (Field field : filter.getClass().getDeclaredFields()) {
            logger.debug("Iterating Field: " + field.getName());

            Method getterMethod;
            Object fieldValue;
            try {
                String getterName = "get" + capitalize(field.getName());
                getterMethod = filter.getClass().getMethod(getterName);
                fieldValue = getterMethod.invoke(filter);
            } catch (Exception e) {
                throw new IllegalAccessException("Error accessing field " + field.getName());
            }

            if (fieldValue == null) {
                continue;
            }

            String fieldName = parentPath != null ? parentPath + "." + field.getName() : field.getName();
            resultSpec = callRequiredSpecificationCreator(resultSpec, fieldValue, fieldName);
        }

        resultSpec = applyLogicalFields(filter, resultSpec, parentPath);
        return resultSpec;
    }

    public static <E> Specification<E> callRequiredSpecificationCreator(Specification<E> resultSpec, @NotNull Object fieldValue, String fieldName) throws IllegalAccessException {
        logger.debug("Building Specification for field: " + fieldName + "; of Type: " + fieldValue.getClass().getSimpleName());

        switch (fieldValue) {
            case StringFilter stringFilter ->
                    resultSpec = resultSpec.and(buildStringSpecification(fieldName, stringFilter));

            case NumericFilter<?> numericFilter ->
                    resultSpec = resultSpec.and(buildNumericSpecification(fieldName, numericFilter));

            case IDFilter idFilter -> resultSpec = resultSpec.and(buildIDSpecification(fieldName, idFilter));

            case BooleanFilter booleanFilter ->
                    resultSpec = resultSpec.and(buildBooleanSpecification(fieldName, booleanFilter));

            case BaseFilter<?> nestedFilter -> resultSpec = resultSpec.and(buildSpecification(nestedFilter, fieldName));

            default -> {
                // Not Needed
            }
        }

        return resultSpec;
    }

    private static <E> Specification<E> applyLogicalFields(@NotNull BaseFilter<?> filter, Specification<E> resultSpec, String parentPath) throws IllegalAccessException {
        if (filter.getAnd() != null && filter.getAnd() instanceof BaseFilter<?> baseFilter) {
            resultSpec = resultSpec.and(buildSpecification(baseFilter, parentPath));
        }

        if (filter.getOr() != null && filter.getOr() instanceof BaseFilter<?> baseFilter) {
            resultSpec = resultSpec.or(buildSpecification(baseFilter, parentPath));
        }

        if (filter.getNot() != null && filter.getNot() instanceof BaseFilter<?> baseFilter) {
            resultSpec = resultSpec.and(Specification.not(buildSpecification(baseFilter, parentPath)));
        }

        return resultSpec;
    }

    private static <E> Specification<E> buildStringSpecification(String fieldName, @NotNull StringFilter filter) {
        logger.debug("String Specification Builder for field: " + fieldName);
        StringSpecificationBuilder<E> builder = new StringSpecificationBuilder<>(fieldName);

        if (filter.getEq() != null) builder.eq(filter.getEq(), filter.getSensitive());
        if (filter.getNe() != null) builder.ne(filter.getNe(), filter.getSensitive());
        if (filter.getIn() != null) builder.in(filter.getIn(), filter.getSensitive());
        if (filter.getNin() != null) builder.nin(filter.getNin(), filter.getSensitive());
        if (filter.getCon() != null) builder.con(filter.getCon(), filter.getSensitive());
        if (filter.getSw() != null) builder.sw(filter.getSw(), filter.getSensitive());
        if (filter.getEw() != null) builder.ew(filter.getEw(), filter.getSensitive());
        if (filter.getLen() != null) builder.len(filter.getLen());

        if (filter.getAnd() != null) builder.and(buildStringSpecification(fieldName, filter.getAnd()));
        if (filter.getOr() != null) builder.or(buildStringSpecification(fieldName, filter.getOr()));
        if (filter.getNot() != null) builder.not(buildStringSpecification(fieldName, filter.getNot()));

        return builder.build();
    }

    private static <E, N extends Number & Comparable<N>> Specification<E> buildNumericSpecification(String fieldName, @NotNull NumericFilter<N> filter) {
        logger.debug("Numeric Specification Builder for field: " + fieldName);
        NumericSpecificationBuilder<E, N> builder = new NumericSpecificationBuilder<>(fieldName);

        if (filter.getEq() != null) builder.eq(filter.getEq());
        if (filter.getNe() != null) builder.ne(filter.getNe());
        if (filter.getIn() != null) builder.in(filter.getIn());
        if (filter.getNin() != null) builder.nin(filter.getNin());
        if (filter.getGt() != null) builder.gt(filter.getGt());
        if (filter.getGe() != null) builder.ge(filter.getGe());
        if (filter.getLt() != null) builder.lt(filter.getLt());
        if (filter.getLe() != null) builder.le(filter.getLe());

        if (filter.getAnd() != null) builder.and(buildNumericSpecification(fieldName, filter.getAnd()));
        if (filter.getOr() != null) builder.or(buildNumericSpecification(fieldName, filter.getOr()));
        if (filter.getNot() != null) builder.not(buildNumericSpecification(fieldName, filter.getNot()));

        return builder.build();
    }

    private static <E> Specification<E> buildIDSpecification(String fieldName, @NotNull IDFilter filter) {
        logger.debug("ID Specification Builder for field: " + fieldName);
        IDSpecificationBuilder<E> builder = new IDSpecificationBuilder<>(fieldName);

        if (filter.getEq() != null) builder.eq(filter.getEq());
        if (filter.getNe() != null) builder.ne(filter.getNe());
        if (filter.getIn() != null) builder.in(filter.getIn());
        if (filter.getNin() != null) builder.nin(filter.getNin());

        if (filter.getAnd() != null) builder.and(buildIDSpecification(fieldName, filter.getAnd()));
        if (filter.getOr() != null) builder.or(buildIDSpecification(fieldName, filter.getOr()));
        if (filter.getNot() != null) builder.not(buildIDSpecification(fieldName, filter.getNot()));

        return builder.build();
    }

    private static <E> Specification<E> buildBooleanSpecification(String fieldName, @NotNull BooleanFilter filter) {
        logger.debug("Boolean Specification Builder for field: " + fieldName);
        BooleanSpecificationBuilder<E> builder = new BooleanSpecificationBuilder<>(fieldName);
        if (filter.getIs() != null) builder.is(filter.getIs());

        return builder.build();
    }
}

