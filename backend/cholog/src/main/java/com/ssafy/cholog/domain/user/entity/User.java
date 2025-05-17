package com.ssafy.cholog.domain.user.entity;

import com.ssafy.cholog.domain.jira.entity.JiraUser;
import com.ssafy.cholog.domain.project.entity.ProjectUser;
import com.ssafy.cholog.global.common.constants.UserType;
import com.ssafy.cholog.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "user_type", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private UserType userType;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private JiraUser jiraUser;

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<ProjectUser> projectUsers = new ArrayList<>();
}
