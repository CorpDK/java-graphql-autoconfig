package com.corpdk.graphql.demo.entity_first.autoconfigurator.specifications;

import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.jpa.domain.Specification;

public class SpecificationBuilder<E> {
    private static final Log logger = LogFactory.getLog(SpecificationBuilder.class);

    @Setter
    protected String field;
    protected Specification<E> specification;

    public SpecificationBuilder(String field) {
        this.specification = Specification.where(null);
        this.field = field;
    }

    public SpecificationBuilder(Specification<E> resultSpec) {
        this.specification = resultSpec;
    }

    public SpecificationBuilder<E> and(Specification<E> spec) {
        logger.debug("Adding clause 'and' for Field: " + field);
        if (spec != null) {
            this.specification = this.specification.and(spec);
        }
        return this;
    }

    public SpecificationBuilder<E> or(Specification<E> spec) {
        logger.debug("Adding clause 'or' for Field: " + field);
        if (spec != null) {
            this.specification = this.specification.or(spec);
        }
        return this;
    }

    public SpecificationBuilder<E> not(Specification<E> spec) {
        logger.debug("Adding clause 'not' for Field: " + field);
        if (spec != null) {
            this.specification = this.specification.and((root, query, cb) -> spec.toPredicate(root, query, cb) != null ? cb.not(spec.toPredicate(root, query, cb)) : cb.conjunction());
        }
        return this;
    }

    public SpecificationBuilder<E> and(SpecificationBuilder<E> spec) {
        logger.debug("Adding clause 'and' for Field: " + field);
        if (spec != null) {
            this.specification = this.specification.and(spec.specification);
        }
        return this;
    }

    public SpecificationBuilder<E> or(SpecificationBuilder<E> spec) {
        logger.debug("Adding clause 'or' for Field: " + field);
        if (spec != null) {
            this.specification = this.specification.or(spec.specification);
        }
        return this;
    }

    public SpecificationBuilder<E> not(SpecificationBuilder<E> spec) {
        logger.debug("Adding clause 'not' for Field: " + field);
        if (spec != null) {
            this.specification = this.specification.and((root, query, cb) -> cb.not(spec.specification.toPredicate(root, query, cb)));
        }
        return this;
    }

    public Specification<E> build() {
        return this.specification;
    }
}

