package com.ssafy.cholog.domain.jira.entity;

import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "jira_project")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JiraProject extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @Column(name = "instance_url", nullable = false)
    private String instanceUrl;

    @Column(name = "project_key", nullable = false, length = 100)
    private String projectKey;

    public void updateJiraProject(String instanceUrl, String projectKey){
        this.instanceUrl = instanceUrl;
        this.projectKey = projectKey;
    }
}
