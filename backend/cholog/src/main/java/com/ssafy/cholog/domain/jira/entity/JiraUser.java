package com.ssafy.cholog.domain.jira.entity;

import com.ssafy.cholog.domain.user.entity.User;
import com.ssafy.cholog.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "jira_user")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JiraUser extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "jira_username", nullable = false)
    private String userName;

    @Column(name = "jira_token", nullable = false, length = 512)
    private String jiraToken;

    public void updateJiraUser(String userName, String jiraToken){
        this.userName = userName;
        this.jiraToken = jiraToken;
    }
}
