package com.corpdk.graphql.demo.entity_first.autoconfigurator.specifications;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class NumericSpecification<E, T extends Number & Comparable<T>> extends BaseSpecification<E> {

    public Specification<E> in(String field, List<T> values) {
        return (root, query, cb) -> values != null && !values.isEmpty() ?
                root.get(field).in(values) : cb.conjunction();
    }

    public Specification<E> nin(String field, List<T> values) {
        return (root, query, cb) -> values != null && !values.isEmpty() ?
                cb.not(root.get(field).in(values)) : cb.conjunction();
    }

    public Specification<E> eq(String field, T value) {
        return (root, query, cb) -> value != null ? cb.equal(root.get(field), value) : cb.conjunction();
    }

    public Specification<E> ne(String field, T value) {
        return (root, query, cb) -> value != null ? cb.notEqual(root.get(field), value) : cb.conjunction();
    }

    public Specification<E> lt(String field, T value) {
        return (root, query, cb) -> value != null ? cb.lessThan(root.get(field), value) : cb.conjunction();
    }

    public Specification<E> le(String field, T value) {
        return (root, query, cb) -> value != null ? cb.lessThanOrEqualTo(root.get(field), value) : cb.conjunction();
    }

    public Specification<E> gt(String field, T value) {
        return (root, query, cb) -> value != null ? cb.greaterThan(root.get(field), value) : cb.conjunction();
    }

    public Specification<E> ge(String field, T value) {
        return (root, query, cb) -> value != null ? cb.greaterThanOrEqualTo(root.get(field), value) : cb.conjunction();
    }
}
