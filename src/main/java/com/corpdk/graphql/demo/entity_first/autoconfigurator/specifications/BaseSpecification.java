package com.corpdk.graphql.demo.entity_first.autoconfigurator.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public abstract class BaseSpecification<E> {
    public Specification<E> and(Specification<E> spec1, Specification<E> spec2) {
        return (root, query, cb) -> {
            Predicate combined = cb.conjunction();
            if (spec1 != null) {
                combined = cb.and(combined, spec1.toPredicate(root, query, cb));
            }
            if (spec2 != null) {
                combined = cb.and(combined, spec2.toPredicate(root, query, cb));
            }
            return combined;
        };
    }

    public Specification<E> or(Specification<E> spec1, Specification<E> spec2) {
        return (root, query, cb) -> {
            Predicate combined = cb.disjunction();
            if (spec1 != null) {
                combined = cb.or(combined, spec1.toPredicate(root, query, cb));
            }
            if (spec2 != null) {
                combined = cb.or(combined, spec2.toPredicate(root, query, cb));
            }
            return combined;
        };
    }

    public Specification<E> not(Specification<E> spec) {
        return (root, query, cb) -> spec != null ? cb.not(spec.toPredicate(root, query, cb)) : cb.conjunction();
    }
}

