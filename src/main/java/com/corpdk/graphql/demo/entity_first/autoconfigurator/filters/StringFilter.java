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
public class StringFilter extends BaseFilter<StringFilter> {
    private List<String> in;
    private List<String> nin;
    private String eq;
    private String ne;
    private String li;
    private String con;
    private String sw;
    private String ew;
    private Long len;
    private Boolean sensitive = true;
}
