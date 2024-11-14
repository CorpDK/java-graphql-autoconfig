package com.corpdk.graphql.demo.entity_first.autoconfigurator.filters;

import lombok.Data;

@Data
@ValidFilter
public class BaseFilter<E> {
    protected E and;
    protected E not;
    protected E or;
}
