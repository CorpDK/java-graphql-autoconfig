package com.corpdk.graphql.demo.entity_first.autoconfigurator.specifications;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

import static com.corpdk.graphql.demo.entity_first.autoconfigurator.Helpers.getPath;

public class NumericSpecificationBuilder<E, N extends Number & Comparable<N>> extends SpecificationBuilder<E> {
    private static final Log logger = LogFactory.getLog(NumericSpecificationBuilder.class);

    public NumericSpecificationBuilder(String field) {
        super(field);
    }

    public NumericSpecificationBuilder<E, N> in(List<N> values) {
        logger.debug("Adding clause 'in' for Field: " + field);
        if (values != null && !values.isEmpty()) {
            this.specification = this.specification.and((root, query, cb) -> getPath(root, field).in(values));
        }
        return this;
    }

    public NumericSpecificationBuilder<E, N> nin(List<N> values) {
        logger.debug("Adding clause 'not in' for Field: " + field);
        if (values != null && !values.isEmpty()) {
            this.specification = this.specification.and((root, query, cb) -> cb.not(getPath(root, field).in(values)));
        }
        return this;
    }

    public NumericSpecificationBuilder<E, N> eq(N value) {
        logger.debug("Adding clause 'equals' for Field: " + field);
        if (value != null) {
            this.specification = this.specification.and((root, query, cb) -> cb.equal(getPath(root, field), value));
        }
        return this;
    }

    public NumericSpecificationBuilder<E, N> ne(N value) {
        logger.debug("Adding clause 'not equals' for Field: " + field);
        if (value != null) {
            this.specification = this.specification.and((root, query, cb) -> cb.notEqual(getPath(root, field), value));
        }
        return this;
    }

    public NumericSpecificationBuilder<E, N> lt(N value) {
        logger.debug("Adding clause 'less than' for Field: " + field);
        if (value != null) {
            this.specification = this.specification.and((root, query, cb) -> cb.lessThan(getPath(root, field), value));
        }
        return this;
    }

    public NumericSpecificationBuilder<E, N> le(N value) {
        logger.debug("Adding clause 'less than or equals' for Field: " + field);
        if (value != null) {
            this.specification = this.specification.and((root, query, cb) -> cb.lessThanOrEqualTo(getPath(root, field), value));
        }
        return this;
    }

    public NumericSpecificationBuilder<E, N> gt(N value) {
        logger.debug("Adding clause 'greater than' for Field: " + field);
        if (value != null) {
            this.specification = this.specification.and((root, query, cb) -> cb.greaterThan(getPath(root, field), value));
        }
        return this;
    }

    public NumericSpecificationBuilder<E, N> ge(N value) {
        logger.debug("Adding clause 'greater than or equals' for Field: " + field);
        if (value != null) {
            this.specification = this.specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(getPath(root, field), value));
        }
        return this;
    }
}

