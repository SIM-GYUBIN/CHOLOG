package com.ssafy.cholog.domain.webhook.service;

import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.project.entity.ProjectUser;
import com.ssafy.cholog.domain.project.repository.ProjectRepository;
import com.ssafy.cholog.domain.project.repository.ProjectUserRepository;
import com.ssafy.cholog.domain.user.entity.User;
import com.ssafy.cholog.domain.user.repository.UserRepository;
import com.ssafy.cholog.domain.webhook.dto.item.WebhookItem;
import com.ssafy.cholog.domain.webhook.dto.request.WebhookRequest;
import com.ssafy.cholog.domain.webhook.dto.response.WebhookResponse;
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

        Webhook webhook = Webhook.builder()
                .project(project)
                .mmURL(request.getMmURL())
                .keywords(request.getKeywords())
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

        webhook.updateSettings(request.getMmURL(), request.getNotificationENV(), request.getKeywords(), request.getIsEnabled());

        return null;
    }

    public WebhookResponse getWebhook(Integer userId, Integer projectId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        Webhook webhook = webhookRepository.findByProject(project).orElse(null);

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", projectId));

        if(webhook == null){
            return WebhookResponse.builder()
                    .exists(false)
                    .build();
        }else{
            return WebhookResponse.builder()
                    .exists(true)
                    .webhookItem(WebhookItem.of(webhook))
                    .build();
        }
    }
}
