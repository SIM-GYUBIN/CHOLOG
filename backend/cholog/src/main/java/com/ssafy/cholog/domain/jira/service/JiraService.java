package com.ssafy.cholog.domain.jira.service;

import com.ssafy.cholog.domain.jira.dto.request.JiraProjectRequest;
import com.ssafy.cholog.domain.jira.dto.request.JiraUserRequest;
import com.ssafy.cholog.domain.jira.dto.response.JiraProjectResponse;
import com.ssafy.cholog.domain.jira.dto.response.JiraUserResponse;
import com.ssafy.cholog.domain.jira.entity.JiraProject;
import com.ssafy.cholog.domain.jira.entity.JiraUser;
import com.ssafy.cholog.domain.jira.repository.JiraProjectRepository;
import com.ssafy.cholog.domain.jira.repository.JiraUserRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JiraService {

    private final UserRepository userRepository;
    private final JiraUserRepository jiraUserRepository;
    private final JiraProjectRepository jiraProjectRepository;
    private final ProjectRepository projectRepository;
    private final ProjectUserRepository projectUserRepository;

    public JiraUserResponse getJiraUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        JiraUser jiraUser = user.getJiraUser();

        if(jiraUser == null){
            return JiraUserResponse.builder()
                    .exists(false)
                    .username(null)
                    .jiraToken(null)
                    .build();
        }else{
            return JiraUserResponse.builder()
                    .exists(true)
                    .username(jiraUser.getUserName())
                    .jiraToken(jiraUser.getJiraToken())
                    .build();
        }
    }

    @Transactional
    public Void registJiraUser(Integer userId, JiraUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        if(user.getJiraUser() != null){
            throw new CustomException(ErrorCode.JIRA_USER_ALREADY_EXISTS, "userId",userId);
        }

        JiraUser jiraUser = JiraUser.builder()
                .user(user)
                .userName(request.getUserName())
                .jiraToken(request.getJiraToken())
                .build();
        jiraUserRepository.save(jiraUser);

        return null;
    }

    @Transactional
    public Void updateJiraUser(Integer userId, JiraUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        JiraUser jiraUser = jiraUserRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.JIRA_USER_NOT_FOUND, "userId",userId));

        jiraUser.updateJiraUser(request.getUserName(), request.getJiraToken());

        return null;
    }

    public JiraProjectResponse getJiraProject(Integer userId, Integer projectId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", project.getId()));

        JiraProject jiraProject = project.getJiraProject();

        if(jiraProject == null){
            return JiraProjectResponse.builder()
                    .exists(false)
                    .instanceUrl(null)
                    .projectKey(null)
                    .build();
        }else{
            return JiraProjectResponse.builder()
                    .exists(true)
                    .instanceUrl(jiraProject.getInstanceUrl())
                    .projectKey(jiraProject.getProjectKey())
                    .build();
        }
    }

    @Transactional
    public Void registJiraProject(Integer userId, Integer projectId, JiraProjectRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", project.getId()));

        if(project.getJiraProject() != null){
            throw new CustomException(ErrorCode.JIRA_PROJECT_ALREADY_EXISTS, "projectId", projectId);
        }

        JiraProject jiraProject =  JiraProject.builder()
                .project(project)
                .instanceUrl(request.getInstanceUrl())
                .projectKey(request.getProjectKey())
                .build();

        jiraProjectRepository.save(jiraProject);

        return null;
    }

    @Transactional
    public Void updateJiraProject(Integer userId, Integer projectId, JiraProjectRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", project.getId()));

        JiraProject jiraProject = jiraProjectRepository.findByProject(project)
                .orElseThrow(() -> new CustomException(ErrorCode.JIRA_PROJECT_NOT_FOUND, "projectId", projectId));

        jiraProject.updateJiraProject(request.getInstanceUrl(), request.getProjectKey());

        return null;
    }
}
