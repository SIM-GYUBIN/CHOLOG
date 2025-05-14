package com.ssafy.cholog.domain.jira.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.cholog.domain.jira.dto.request.JiraIssueRequest;
import com.ssafy.cholog.domain.jira.dto.request.JiraRequest;
import com.ssafy.cholog.domain.jira.dto.response.JiraResponse;
import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.project.entity.ProjectUser;
import com.ssafy.cholog.domain.project.repository.ProjectRepository;
import com.ssafy.cholog.domain.project.repository.ProjectUserRepository;
import com.ssafy.cholog.domain.user.entity.User;
import com.ssafy.cholog.domain.user.repository.UserRepository;
import com.ssafy.cholog.global.exception.CustomException;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JiraService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectUserRepository projectUserRepository;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public JiraResponse getJiraToken(Integer userId, Integer projectId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", projectId));

        String jiraToken = project.getJiraToken();

        if(jiraToken == null){
            return JiraResponse.builder()
                    .exists(false)
                    .jiraToken(null)
                    .build();
        }else{
            return JiraResponse.builder()
                    .exists(true)
                    .jiraToken(jiraToken)
                    .build();
        }
    }

    @Transactional
    public Void registJiraToken(Integer userId, Integer projectId, JiraRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", projectId));

        if(project.getJiraToken() != null){
            throw new CustomException(ErrorCode.JIRATOKEN_ALREADY_EXISTS)
                    .addParameter("projectId", projectId);
        }

        project.updateJiraToken(request.getJiraToken());

        return null;
    }

    @Transactional
    public Void updateJiraToken(Integer userId, Integer projectId, JiraRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", projectId));

        if(project.getJiraToken() == null){
            throw new CustomException(ErrorCode.JIRATOKEN_NOT_EXISTS)
                    .addParameter("projectId", projectId);
        }

        project.updateJiraToken(request.getJiraToken());

        return null;
    }
}
