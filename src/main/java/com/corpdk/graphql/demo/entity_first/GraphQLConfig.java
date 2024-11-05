package com.corpdk.graphql.demo.entity_first;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
//@Configuration(proxyBeanMethods = false)
@RestController()
public class GraphQLConfig {

    @GetMapping("apollo")
    public ResponseEntity<Void> openApolloSandboxExplorer() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "https://studio.apollographql.com/sandbox/explorer?endpoint=http%3A%2F%2Flocalhost%3A8080%2Fgraphql")
                .build();
    }
}
