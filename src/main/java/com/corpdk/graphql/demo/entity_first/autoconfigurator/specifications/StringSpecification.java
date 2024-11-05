package com.corpdk.graphql.demo.entity_first.autoconfigurator.specifications;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class StringSpecification<E> extends BaseSpecification<E> {

    public Specification<E> in(String field, List<String> values, Boolean sensitive) {
        return (root, query, cb) -> {
            if (values == null || values.isEmpty()) {
                return cb.conjunction();
            }
            if (Boolean.TRUE.equals(sensitive)) {
                return root.get(field).in(values);
            } else {
                List<String> lowerValues = values.stream()
                        .map(String::toLowerCase)
                        .toList();
                return cb.lower(root.get(field)).in(lowerValues);
            }
        };
    }

    public Specification<E> nin(String field, List<String> values, Boolean sensitive) {
        return (root, query, cb) -> {
            if (values == null || values.isEmpty()) {
                return cb.conjunction();
            }
            if (Boolean.TRUE.equals(sensitive)) {
                return cb.not(root.get(field).in(values));
            } else {
                List<String> lowerValues = values.stream()
                        .map(String::toLowerCase)
                        .toList();
                return cb.not(cb.lower(root.get(field)).in(lowerValues));
            }
        };
    }

    public Specification<E> eq(String field, String value, Boolean sensitive) {
        return (root, query, cb) -> {
            if (value != null) {
                return Boolean.TRUE.equals(sensitive) ? cb.equal(root.get(field), value) :
                        cb.equal(cb.lower(root.get(field)), value.toLowerCase());
            }
            return cb.conjunction();
        };
    }

    public Specification<E> ne(String field, String value, Boolean sensitive) {
        return (root, query, cb) -> {
            if (value != null) {
                return Boolean.TRUE.equals(sensitive) ? cb.notEqual(root.get(field), value) :
                        cb.notEqual(cb.lower(root.get(field)), value.toLowerCase());
            }
            return cb.conjunction();
        };
    }

    public Specification<E> like(String field, String pattern, Boolean sensitive) {
        return (root, query, cb) -> {
            if (pattern != null) {
                return Boolean.TRUE.equals(sensitive) ? cb.like(root.get(field), pattern) :
                        cb.like(cb.lower(root.get(field)), pattern.toLowerCase());
            }
            return cb.conjunction();
        };
    }

    public Specification<E> contains(String field, String substring, Boolean sensitive) {
        return (root, query, cb) -> {
            if (substring != null) {
                return Boolean.TRUE.equals(sensitive) ? cb.like(root.get(field), "%" + substring + "%") :
                        cb.like(cb.lower(root.get(field)), "%" + substring.toLowerCase() + "%");
            }
            return cb.conjunction();
        };
    }

    public Specification<E> startsWith(String field, String prefix, Boolean sensitive) {
        return (root, query, cb) -> {
            if (prefix != null) {
                return Boolean.TRUE.equals(sensitive) ? cb.like(root.get(field), prefix + "%") :
                        cb.like(cb.lower(root.get(field)), prefix.toLowerCase() + "%");
            }
            return cb.conjunction();
        };
    }

    public Specification<E> endsWith(String field, String suffix, Boolean sensitive) {
        return (root, query, cb) -> {
            if (suffix != null) {
                return Boolean.TRUE.equals(sensitive) ? cb.like(root.get(field), "%" + suffix) :
                        cb.like(cb.lower(root.get(field)), "%" + suffix.toLowerCase());
            }
            return cb.conjunction();
        };
    }

    public Specification<E> len(String field, Integer length) {
        return (root, query, cb) -> length != null ?
                cb.equal(cb.length(root.get(field)), length) : cb.conjunction();
    }
}
