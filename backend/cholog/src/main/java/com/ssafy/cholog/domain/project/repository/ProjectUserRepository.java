package com.ssafy.cholog.domain.project.repository;

import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.project.entity.ProjectUser;
import com.ssafy.cholog.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectUserRepository extends JpaRepository<ProjectUser, Integer> {

    boolean existsByProjectAndUser(Project project, User user);

    List<ProjectUser> findByUserOrderByProjectCreatedAtDesc(User user);
}
