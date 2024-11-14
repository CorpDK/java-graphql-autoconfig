package com.corpdk.graphql.demo.entity_first.autoconfigurator.filters;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class NumericFilter<T extends Number & Comparable<T>> extends BaseFilter<NumericFilter<T>> {
    private List<T> in;
    private List<T> nin;
    private T eq;
    private T ne;
    private T lt;
    private T le;
    private T gt;
    private T ge;
}
