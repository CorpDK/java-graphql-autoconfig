package com.corpdk.graphql.demo.entity_first.autoconfigurator.filters;

import com.corpdk.graphql.demo.entity_first.autoconfigurator.filters.validators.FilterValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = {FilterValidator.class})
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFilter {
    String message() default "If 'not' is not null, no other field should have data.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
