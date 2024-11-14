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
public class IDFilter extends BaseFilter<IDFilter> {
    private List<String> in;
    private List<String> nin;
    private String eq;
    private String ne;
}
