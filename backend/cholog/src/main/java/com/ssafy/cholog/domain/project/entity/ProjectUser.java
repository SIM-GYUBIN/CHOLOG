package com.ssafy.cholog.domain.project.entity;

import com.ssafy.cholog.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_project_user",
                        columnNames = {"project_id", "user_id"}
                )
        })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProjectUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "is_creator")
    private Boolean isCreator = false;
}
