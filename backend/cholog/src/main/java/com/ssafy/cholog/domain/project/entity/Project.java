package com.ssafy.cholog.domain.project.entity;

import com.ssafy.cholog.domain.jira.entity.JiraProject;
import com.ssafy.cholog.domain.log.entity.LogArchive;
import com.ssafy.cholog.domain.webhook.entity.Webhook;
import com.ssafy.cholog.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project",
        indexes = {
                @Index(name = "idx_project_token", columnList = "projectToken")
        })
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Project extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name="project_token", nullable = false)
    private String projectToken;

    @Builder.Default
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ProjectUser> projectUsers = new ArrayList<>();

    @OneToOne(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private JiraProject jiraProject;

    @OneToOne(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Webhook webhook;

    @Builder.Default
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<LogArchive> logArchives = new ArrayList<>();

    public void updateProjectToken(String token){
        this.projectToken = token;
    }

    public void updateProjectName(String name) { this.name = name; }
}
