package com.corpdk.graphql.demo.entity_first;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.graphql.data.GraphQlQueryByExampleAutoConfiguration;

@SpringBootApplication(exclude = {GraphQlQueryByExampleAutoConfiguration.class})
public class EntityFirstApplication {

    public static void main(String[] args) {
        SpringApplication.run(EntityFirstApplication.class, args);
    }

}
