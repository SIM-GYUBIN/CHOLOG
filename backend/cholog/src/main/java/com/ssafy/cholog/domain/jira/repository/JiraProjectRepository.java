package com.ssafy.cholog.domain.jira.repository;

import com.ssafy.cholog.domain.jira.entity.JiraProject;
import com.ssafy.cholog.domain.jira.entity.JiraUser;
import com.ssafy.cholog.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JiraProjectRepository extends JpaRepository<JiraProject, Integer> {

    Optional<JiraProject> findByProject(Project project);
}
