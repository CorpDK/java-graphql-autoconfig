package com.corpdk.graphql.demo.entity_first.autoconfigurator;

import com.corpdk.graphql.demo.entity_first.autoconfigurator.fetchers.JpaSpecificationDataFetcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
//@AutoConfiguration(after = GraphQlAutoConfiguration.class)
//@ConditionalOnClass({ GraphQL.class, JpaSpecificationDataFetcher.class, JpaSpecificationExecutor.class })
//@ConditionalOnBean(GraphQlSource.class)
public class FilterJpaSpecificationExecutorAutoConfiguration {
    private static final Log logger = LogFactory.getLog(FilterJpaSpecificationExecutorAutoConfiguration.class);

    @Bean
    public GraphQlSourceBuilderCustomizer jpaSpecificationRegistrar(ObjectProvider<JpaSpecificationExecutor<?>> executors) {
        logger.info("My GraphQlSourceBuilderCustomizer: jpaSpecificationRegistrar");
        RuntimeWiringConfigurer configurer = JpaSpecificationDataFetcher
                .autoRegistrationConfigurer(executors.orderedStream().toList());
        return builder -> builder.configureRuntimeWiring(configurer);
    }
}
