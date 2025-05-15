package com.ssafy.cholog.domain.jira.repository;

import com.ssafy.cholog.domain.jira.entity.JiraUser;
import com.ssafy.cholog.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JiraUserRepository extends JpaRepository<JiraUser, Integer> {

    Optional<JiraUser> findByUser(User user);
}
