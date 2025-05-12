package com.ssafy.cholog.domain.webhook.service;

import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.project.entity.ProjectUser;
import com.ssafy.cholog.domain.project.repository.ProjectRepository;
import com.ssafy.cholog.domain.project.repository.ProjectUserRepository;
import com.ssafy.cholog.domain.user.entity.User;
import com.ssafy.cholog.domain.user.repository.UserRepository;
import com.ssafy.cholog.domain.webhook.dto.request.WebhookRequest;
import com.ssafy.cholog.domain.webhook.entity.Webhook;
import com.ssafy.cholog.domain.webhook.repository.WebhookRepository;
import com.ssafy.cholog.global.exception.CustomException;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebhookService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectUserRepository projectUserRepository;
    private final WebhookRepository webhookRepository;

    @Transactional
    public Void createWebhook(Integer userId, Integer projectId, WebhookRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        if(webhookRepository.findByProject(project).isPresent()){
            throw new CustomException(ErrorCode.WEBHOOK_ALREADY_EXISTS, "projectId", projectId);
        }

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", projectId));

        // 해당 유저가 프로젝트 생성자인지 확인
        if (!projectUser.getIsCreator()) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS, "웹훅 설정 권한이 없습니다.");
        }

        Webhook webhook = Webhook.builder()
                .project(project)
                .mmURL(request.getMmURL())
                .logLevel(request.getLogLevel())
                .notificationENV(request.getNotificationENV())
                .isEnabled(request.getIsEnabled())
                .build();
        webhookRepository.save(webhook);
        
        return null;
    }

    @Transactional
    public Void updateWebhook(Integer userId, Integer projectId, WebhookRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        Webhook webhook = webhookRepository.findByProject(project)
                .orElseThrow(() -> new CustomException(ErrorCode.WEBHOOK_NOT_FOUND, "projectId",projectId));

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", projectId));

        // 해당 유저가 프로젝트 생성자인지 확인
        if (!projectUser.getIsCreator()) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS, "웹훅 수정 권한이 없습니다.");
        }

        webhook.updateMmURL(request.getMmURL());
        webhook.updateLogLevel(request.getLogLevel());
        webhook.updateNotificationENV(request.getNotificationENV());
        webhook.updateIsEnabled(request.getIsEnabled());

        return null;
    }
}
