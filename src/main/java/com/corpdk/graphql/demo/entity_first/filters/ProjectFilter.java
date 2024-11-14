package com.corpdk.graphql.demo.entity_first.filters;

import com.corpdk.graphql.demo.entity_first.autoconfigurator.filters.BaseFilter;
import com.corpdk.graphql.demo.entity_first.autoconfigurator.filters.NumericFilter;
import com.corpdk.graphql.demo.entity_first.autoconfigurator.filters.StringFilter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class ProjectFilter extends BaseFilter<ProjectFilter> {
    private NumericFilter<Long> id;

    private StringFilter projectName;

    private EmployeeFilter employee;
}

