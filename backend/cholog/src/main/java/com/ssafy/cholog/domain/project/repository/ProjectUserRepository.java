package com.ssafy.cholog.domain.project.repository;

import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.project.entity.ProjectUser;
import com.ssafy.cholog.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectUserRepository extends JpaRepository<ProjectUser, Integer> {

    Optional<ProjectUser> findByUserIdAndProjectId(Integer userId, Integer projectId);
    boolean existsByProjectAndUser(Project project, User user);

    @EntityGraph(attributePaths = {"project"})
    List<ProjectUser> findByUserOrderByProjectCreatedAtDesc(User user);

    @EntityGraph(attributePaths = {"project", "user"})
    Optional<ProjectUser> findByUserAndProject(User user, Project project);

    boolean existsByProjectIdAndUserId(Integer projectId, Integer userId);

    @EntityGraph(attributePaths = {"user"})
    List<ProjectUser> findAllByProject(Project project);
}
