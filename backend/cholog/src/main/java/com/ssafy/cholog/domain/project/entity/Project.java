package com.ssafy.cholog.domain.project.entity;

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

    @Column(name="mm_url")
    private String mmURL;

    @Column(name="jira_token")
    private String jiraToken;

    @Builder.Default
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ProjectUser> projectUsers = new ArrayList<>();

    public void updateProjectToken(String token){
        this.projectToken = token;
    }

    public void updateProjectName(String name) { this.name = name; }
    public void updateMmURL(String url) { this.mmURL = url; }
    public void updateJiraToken(String token) { this.jiraToken = token; }
}
