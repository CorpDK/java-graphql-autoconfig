package com.corpdk.graphql.demo.entity_first.autoconfigurator.filters.validators;

import com.corpdk.graphql.demo.entity_first.autoconfigurator.filters.BaseFilter;
import com.corpdk.graphql.demo.entity_first.autoconfigurator.filters.ValidFilter;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.SneakyThrows;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class FilterValidator implements ConstraintValidator<ValidFilter, Object> {
    private static final Log logger = LogFactory.getLog(FilterValidator.class);

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public void initialize(ValidFilter constraintAnnotation) {
        // No initialization needed
    }

    @SneakyThrows
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (!(value instanceof BaseFilter<?> filter)) {
            return true;
        }
        logger.debug("Running Custom Validator for class: " + filter.getClass().getSimpleName());
        return validateFilter(filter, context, "");
    }

    private boolean validateFilter(@NotNull BaseFilter<?> filter, ConstraintValidatorContext context, String parentPath) throws IllegalAccessException {
        boolean isValid = true;
        boolean notIsDefined = filter.getNot() != null;

        logger.debug("Validating: " + filter.getClass().getSimpleName());
        for (Field field : filter.getClass().getDeclaredFields()) {
            if ("sensitive".equals(field.getName())) {
                continue;
            }
            try {
                logger.debug("Iterating Field: " + field.getName());
                String getterName = "get" + capitalize(field.getName());

                Method getterMethod = filter.getClass().getMethod(getterName);

                Object fieldValue = getterMethod.invoke(filter);

                if (notIsDefined) {
                    isValid = checkField(field, fieldValue, context, parentPath, isValid);
                }

                if (fieldValue instanceof BaseFilter<?> nestedFilter) {
                    String nestedPath = parentPath.isEmpty() ? field.getName() : parentPath + "." + field.getName();
                    isValid = validateFilter(nestedFilter, context, nestedPath) && isValid;
                }
            } catch (Exception e) {
                throw new IllegalAccessException("Error accessing field " + field.getName());
            }
        }

        isValid = checkBaseNestedFields(filter, context, parentPath, isValid);

        return isValid;
    }

    private boolean checkBaseNestedFields(@NotNull BaseFilter<?> filter, ConstraintValidatorContext context, String parentPath, boolean isValid) throws IllegalAccessException {
        if (filter.getAnd() instanceof BaseFilter<?> nestedFilter) {
            String nestedPath = parentPath.isEmpty() ? "and" : parentPath + ".and";
            isValid = validateFilter(nestedFilter, context, nestedPath) && isValid;
        }
        if (filter.getOr() instanceof BaseFilter<?> nestedFilter) {
            String nestedPath = parentPath.isEmpty() ? "or" : parentPath + ".or";
            isValid = validateFilter(nestedFilter, context, nestedPath) && isValid;
        }
        if (filter.getNot() instanceof BaseFilter<?> nestedFilter) {
            String nestedPath = parentPath.isEmpty() ? "not" : parentPath + ".not";
            isValid = validateFilter(nestedFilter, context, nestedPath) && isValid;
        }

        return isValid;
    }

    private boolean checkField(@NotNull Field field, Object fieldValue, ConstraintValidatorContext context, String parentPath, boolean isValid) {
        if (!field.getName().equals("not") && fieldValue != null && !isEmptyValue(fieldValue)) {
            addValidationMessage(field, context, parentPath);
            isValid = false;
        }
        return isValid;
    }

    private void addValidationMessage(Field field, @NotNull ConstraintValidatorContext context, @NotNull String parentPath) {
        context.disableDefaultConstraintViolation();

        String fieldPath = parentPath.isEmpty() ? field.getName() : parentPath + "." + field.getName();
        String notPath = parentPath.isEmpty() ? "not" : parentPath + ".not";
        String customMessage = String.format("Field '%s' must be empty if '%s' is not null.", fieldPath, notPath);
        context.buildConstraintViolationWithTemplate(customMessage)
                .addPropertyNode(fieldPath)
                .addConstraintViolation();
    }

    private boolean isEmptyValue(Object value) {
        if (value instanceof List<?> list) {
            return list.isEmpty();
        } else if (value instanceof String string) {
            return string.isEmpty();
        }
        return false;
    }
}

