package com.corpdk.graphql.demo.entity_first.autoconfigurator.filters;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class StringFilter {
    private List<String> in;
    private List<String> nin;
    private String eq;
    private String ne;
    private String like;
    private String contains;
    private String startsWith;
    private String endsWith;
    private Integer len;
    private Boolean sensitive;
    private StringFilter and;
    private StringFilter not;
    private StringFilter or;
}
