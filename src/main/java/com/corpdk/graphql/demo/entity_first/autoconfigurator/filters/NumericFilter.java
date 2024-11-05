package com.corpdk.graphql.demo.entity_first.autoconfigurator.filters;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
public class NumericFilter<T extends Number & Comparable<T>> {
    private List<T> in;
    private List<T> nin;
    private T eq;
    private T ne;
    private T lt;
    private T le;
    private T gt;
    private T ge;
    private NumericFilter<T> and;
    private NumericFilter<T> not;
    private NumericFilter<T> or;
}
