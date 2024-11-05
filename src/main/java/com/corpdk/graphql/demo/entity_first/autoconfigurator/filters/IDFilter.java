package com.corpdk.graphql.demo.entity_first.autoconfigurator.filters;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class IDFilter {
    private List<String> in;
    private List<String> nin;
    private String eq;
    private String ne;
    private IDFilter and;
    private IDFilter not;
    private IDFilter or;
}
