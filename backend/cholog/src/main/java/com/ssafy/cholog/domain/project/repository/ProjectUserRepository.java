package com.ssafy.cholog.domain.project.repository;

import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.project.entity.ProjectUser;
import com.ssafy.cholog.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.util.List;

@Repository
public interface ProjectUserRepository extends JpaRepository<ProjectUser, Integer> {

    Optional<ProjectUser> findByUserIdAndProjectId(Integer userId, Integer projectId);
    boolean existsByProjectAndUser(Project project, User user);

    List<ProjectUser> findByUserOrderByProjectCreatedAtDesc(User user);

    ProjectUser findByUserAndProject(User user, Project project);
}
