package com.corpdk.graphql.demo.entity_first.autoconfigurator.specifications;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.corpdk.graphql.demo.entity_first.autoconfigurator.Helpers.getPath;

public class StringSpecificationBuilder<E> extends SpecificationBuilder<E> {
    private static final Log logger = LogFactory.getLog(StringSpecificationBuilder.class);

    public StringSpecificationBuilder(String field) {
        super(field);
    }

    @Contract("_, _, _, _ -> new")
    private @NotNull Sensitivity applyCaseSensitivity(Path<String> path, String value, Boolean caseSensitive, CriteriaBuilder cb) {
        if (Boolean.TRUE.equals(caseSensitive)) {
            return new Sensitivity(path, value);
        } else {
            return new Sensitivity(cb.lower(path), value.toLowerCase());
        }
    }

    @Contract("_, _, _, _ -> new")
    private @NotNull SensitivityList applyCaseSensitivity(Path<String> path, List<String> values, Boolean caseSensitive, CriteriaBuilder cb) {
        if (Boolean.TRUE.equals(caseSensitive)) {
            return new SensitivityList(path, values);
        } else {
            return new SensitivityList(cb.lower(path), values.stream().map(String::toLowerCase).toList());
        }
    }

    public StringSpecificationBuilder<E> in(List<String> values, Boolean caseSensitive) {
        logger.debug("Adding clause 'in' for Field: " + field);
        if (values != null && !values.isEmpty()) {
            this.specification = this.specification.and((root, query, cb) -> {
                SensitivityList criteria = applyCaseSensitivity(getPath(root, field), values, caseSensitive, cb);
                return criteria.path().in(criteria.values());
            });
        }
        return this;
    }

    public StringSpecificationBuilder<E> nin(List<String> values, Boolean caseSensitive) {
        logger.debug("Adding clause 'not in' for Field: " + field);
        if (values != null && !values.isEmpty()) {
            this.specification = this.specification.and((root, query, cb) -> {
                SensitivityList criteria = applyCaseSensitivity(getPath(root, field), values, caseSensitive, cb);
                return cb.not(criteria.path().in(criteria.values()));
            });
        }
        return this;
    }

    public StringSpecificationBuilder<E> eq(String value, Boolean caseSensitive) {
        logger.debug("Adding clause 'equals' for Field: " + field);
        if (value != null) {
            this.specification = this.specification.and((root, query, cb) -> {
                Sensitivity criteria = applyCaseSensitivity(getPath(root, field), value, caseSensitive, cb);
                return cb.equal(criteria.path(), criteria.value());
            });
        }
        return this;
    }

    public StringSpecificationBuilder<E> ne(String value, Boolean caseSensitive) {
        logger.debug("Adding clause 'not equals' for Field: " + field);
        if (value != null) {
            this.specification = this.specification.and((root, query, cb) -> {
                Sensitivity criteria = applyCaseSensitivity(getPath(root, field), value, caseSensitive, cb);
                return cb.notEqual(criteria.path(), criteria.value());
            });
        }
        return this;
    }

    public StringSpecificationBuilder<E> li(String value, Boolean caseSensitive) {
        logger.debug("Adding clause 'like' for Field: " + field);
        if (value != null) {
            this.specification = this.specification.and((root, query, cb) -> {
                Sensitivity criteria = applyCaseSensitivity(getPath(root, field), value, caseSensitive, cb);
                return cb.like(criteria.path(), criteria.value());
            });
        }
        return this;
    }

    public StringSpecificationBuilder<E> con(String value, Boolean caseSensitive) {
        logger.debug("Adding clause 'contains' for Field: " + field);
        return li("%" + value + "%", caseSensitive);
    }

    public StringSpecificationBuilder<E> sw(String value, Boolean caseSensitive) {
        logger.debug("Adding clause 'starts with' for Field: " + field);
        return li(value + "%", caseSensitive);
    }

    public StringSpecificationBuilder<E> ew(String value, Boolean caseSensitive) {
        logger.debug("Adding clause 'ends with' for Field: " + field);
        return li("%" + value, caseSensitive);
    }

    public StringSpecificationBuilder<E> len(Long length) {
        logger.debug("Adding clause 'length' for Field: " + field);
        if (length != null) {
            this.specification = this.specification.and((root, query, cb) -> cb.equal(cb.length(root.get(field)), length));
        }
        return this;
    }

    record Sensitivity(Expression<String> path, String value) {
    }

    record SensitivityList(Expression<String> path, List<String> values) {
    }
}

