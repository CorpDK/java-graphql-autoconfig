package com.corpdk.graphql.demo.entity_first.autoconfigurator.specifications;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

import static com.corpdk.graphql.demo.entity_first.autoconfigurator.Helpers.getPath;

public class IDSpecificationBuilder<E> extends SpecificationBuilder<E> {
    private static final Log logger = LogFactory.getLog(IDSpecificationBuilder.class);

    public IDSpecificationBuilder(String field) {
        super(field);
    }

    public IDSpecificationBuilder<E> in(List<String> values) {
        logger.debug("Adding clause 'in' for Field: " + field);
        if (values != null && !values.isEmpty()) {
            this.specification = this.specification.and((root, query, cb) -> getPath(root, field).in(values));
        }
        return this;
    }

    public IDSpecificationBuilder<E> nin(List<String> values) {
        logger.debug("Adding clause 'not in' for Field: " + field);
        if (values != null && !values.isEmpty()) {
            this.specification = this.specification.and((root, query, cb) -> cb.not(getPath(root, field).in(values)));
        }
        return this;
    }

    public IDSpecificationBuilder<E> eq(String value) {
        logger.debug("Adding clause 'equals' for Field: " + field);
        if (value != null) {
            this.specification = this.specification.and((root, query, cb) -> cb.equal(getPath(root, field), value));
        }
        return this;
    }

    public IDSpecificationBuilder<E> ne(String value) {
        logger.debug("Adding clause 'not equals' for Field: " + field);
        if (value != null) {
            this.specification = this.specification.and((root, query, cb) -> cb.notEqual(getPath(root, field), value));
        }
        return this;
    }
}
