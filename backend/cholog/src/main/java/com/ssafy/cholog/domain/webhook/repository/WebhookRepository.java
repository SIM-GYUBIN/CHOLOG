package com.ssafy.cholog.domain.webhook.repository;

import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.webhook.entity.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebhookRepository extends JpaRepository<Webhook, Integer> {
    Optional<Webhook> findByProject(Project project);

    List<Webhook> findByIsEnabledTrue();
}
