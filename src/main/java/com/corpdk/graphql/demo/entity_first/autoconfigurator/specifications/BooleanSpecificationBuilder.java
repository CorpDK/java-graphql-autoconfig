package com.corpdk.graphql.demo.entity_first.autoconfigurator.specifications;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static com.corpdk.graphql.demo.entity_first.autoconfigurator.Helpers.getPath;

public class BooleanSpecificationBuilder<E> extends SpecificationBuilder<E> {
    private static final Log logger = LogFactory.getLog(BooleanSpecificationBuilder.class);
    public BooleanSpecificationBuilder(String field) {
        super(field);
    }

    public BooleanSpecificationBuilder<E> is(Boolean value) {
        logger.debug("Adding clause 'is' for Field: " + field);
        if (value != null) {
            this.specification = this.specification.and((root, query, cb) -> cb.equal(getPath(root, field), value));
        }
        return this;
    }
}
