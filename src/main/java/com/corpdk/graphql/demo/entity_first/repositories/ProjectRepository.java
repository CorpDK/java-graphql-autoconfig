package com.corpdk.graphql.demo.entity_first.repositories;

import com.corpdk.graphql.demo.entity_first.models.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.graphql.data.GraphQlRepository;

@GraphQlRepository
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
}