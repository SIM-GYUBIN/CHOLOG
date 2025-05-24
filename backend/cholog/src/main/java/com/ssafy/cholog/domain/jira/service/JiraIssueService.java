package com.ssafy.cholog.domain.jira.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.cholog.domain.jira.dto.payload.JiraIssueCreatePayload;
import com.ssafy.cholog.domain.jira.dto.payload.JiraIssueFieldsPayload;
import com.ssafy.cholog.domain.jira.dto.payload.JiraIssueIdentifierPayload;
import com.ssafy.cholog.domain.jira.dto.request.JiraIssueRequest;
import com.ssafy.cholog.domain.jira.dto.response.JiraIssueCreationResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JiraIssueService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final JiraUserRepository jiraUserRepository;
    private final JiraProjectRepository jiraProjectRepository;
    private final ProjectUserRepository projectUserRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public JiraIssueCreationResponse createJiraIssue(Integer userId, Integer projectId, JiraIssueRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", project.getId()));

        JiraUser jiraUser = jiraUserRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.JIRA_USER_NOT_FOUND, "userId", userId));

        String jiraApiUsername = jiraUser.getUserName();
        String jiraToken = jiraUser.getJiraToken();

        JiraProject jiraProject = jiraProjectRepository.findByProject(project)
                .orElseThrow(() -> new CustomException(ErrorCode.JIRA_PROJECT_NOT_FOUND, "projectId", projectId));

        String jiraInstanceUrl = jiraProject.getInstanceUrl();
        String jiraProjectKey = jiraProject.getProjectKey();

        // Jira API 요청 본문 생성
        JiraIssueFieldsPayload.JiraIssueFieldsPayloadBuilder fieldsBuilder = JiraIssueFieldsPayload.builder()
                .project(JiraIssueIdentifierPayload.builder().key(jiraProjectKey).build())
                .summary(request.getSummary())
                .issuetype(JiraIssueIdentifierPayload.builder().name(request.getIssueType()).build());

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            fieldsBuilder.description(request.getDescription());
        }

        // 보고자(Reporter) 설정
        String reporterAccountIdToUse = null;
        if (request.getReporterName() != null && !request.getReporterName().isBlank()) {
            reporterAccountIdToUse = fetchAccountIdByReporterIdentifier(jiraInstanceUrl, request.getReporterName(), jiraApiUsername, jiraToken);
            if (reporterAccountIdToUse == null) {
                log.warn("Could not fetch accountId for reporterName: {}. Falling back or issue creation might fail.", request.getReporterName());
            }
        }

        if (reporterAccountIdToUse != null) {
            fieldsBuilder.reporter(JiraIssueIdentifierPayload.builder().accountId(reporterAccountIdToUse).build());
        } else if (jiraApiUsername != null && !jiraApiUsername.isBlank()) {
            fieldsBuilder.reporter(JiraIssueIdentifierPayload.builder().name(jiraApiUsername).build());
        } else {
            log.error("Reporter could not be determined. Issue creation will likely fail if reporter is required.");
        }

        // 담당자(Assignee) 설정
        String assigneeAccountIdToUse = null;
        if (request.getAssigneeName() != null && !request.getAssigneeName().isBlank()) {
            assigneeAccountIdToUse = fetchAccountIdByReporterIdentifier(
                    jiraInstanceUrl,
                    request.getAssigneeName(),
                    jiraApiUsername,
                    jiraToken
            );
            if (assigneeAccountIdToUse == null) {
                log.warn("Could not fetch accountId for assignee identifier: {}. Assignee will not be set by this identifier.", request.getAssigneeName());
            }
        }

        if (assigneeAccountIdToUse != null) {
            fieldsBuilder.assignee(JiraIssueIdentifierPayload.builder().accountId(assigneeAccountIdToUse).build());
        } else {
            log.info("No valid assignee accountId could be determined. Issue will follow Jira's default assignment rules or remain unassigned if allowed.");
        }

        JiraIssueFieldsPayload fieldsPayload = fieldsBuilder.build();

        JiraIssueCreatePayload jiraPayloadObject = JiraIssueCreatePayload.builder()
                .fields(fieldsPayload)
                .build();

        String jiraPayloadJson;
        try {
            // ObjectMapper로 생성된 객체를 JSON 문자열로 변환
            jiraPayloadJson = objectMapper.writeValueAsString(jiraPayloadObject);
        } catch (JsonProcessingException e) {
            log.error("Error creating Jira issue payload JSON: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Jira 요청 데이터 생성 중 오류 발생");
        }

        // HTTP 요청 생성 및 전송
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Basic Authentication: Jira 사용자 이름과 API 토큰 사용
        String auth = jiraApiUsername + ":" + jiraToken;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON)); // 응답 타입을 JSON으로 기대

        HttpEntity<String> entity = new HttpEntity<>(jiraPayloadJson, headers);

        // Jira API 엔드포인트
        String jiraApiEndpoint = jiraInstanceUrl.endsWith("/") ? jiraInstanceUrl : jiraInstanceUrl + "/";
        jiraApiEndpoint += "rest/api/2/issue";

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(jiraApiEndpoint, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) { // 일반적으로 201 Created
                log.info("Jira issue created successfully. Status: {}, Response: {}", response.getStatusCode(), response.getBody());
                String responseBody = response.getBody();
                try {
                    JsonNode rootNode = objectMapper.readTree(responseBody);
                    String issueKey = rootNode.path("key").asText();

                    // 사용자에게 보여줄 Jira 이슈 URL 생성
                    String userViewableIssueUrl = jiraInstanceUrl.endsWith("/") ? jiraInstanceUrl : jiraInstanceUrl + "/";
                    userViewableIssueUrl += "browse/" + issueKey;

                    return new JiraIssueCreationResponse(issueKey, userViewableIssueUrl);

                } catch (JsonProcessingException e) {
                    log.error("Failed to parse Jira issue creation response: {}", responseBody, e);
                    // 이슈는 생성되었지만, 응답 파싱에 실패한 경우.
                    // 여기서는 에러를 던져서 트랜잭션 롤백 등을 유도 (만약 DB 변경이 있었다면)
                    throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Jira 이슈는 생성되었으나 응답을 파싱하는데 실패했습니다.");
                }
            } else {
                log.error("Jira issue creation returned non-2xx success status: {}. Response: {}", response.getStatusCode(), response.getBody());
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Jira 이슈 생성에 실패했으나, 응답 코드가 예상과 다릅니다: " + response.getStatusCodeValue());
            }
        } catch (HttpClientErrorException e) {
            String errorResponseBody = e.getResponseBodyAsString();
            log.error("Jira API Client Error (4xx): Status Code: {}, Message: {}, Response Body: {}", e.getStatusCode(), e.getMessage(), errorResponseBody, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Jira API 요청 오류: " + extractJiraErrorMessage(errorResponseBody, e.getStatusCode().value()));
        } catch (HttpServerErrorException e) {
            String errorResponseBody = e.getResponseBodyAsString();
            log.error("Jira API Server Error (5xx): Status Code: {}, Message: {}, Response Body: {}", e.getStatusCode(), e.getMessage(), errorResponseBody, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Jira 서버에서 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("Unexpected error during Jira issue creation: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Jira 이슈 생성 중 예기치 않은 오류 발생: " + e.getMessage());
        }
    }

    private String fetchAccountIdByReporterIdentifier(String jiraInstanceUrl, String reporterIdentifier,
                                                      String authUsername, String authToken) {
        if (reporterIdentifier == null || reporterIdentifier.isBlank()) {
            return null;
        }

        String userSearchUrl = jiraInstanceUrl.endsWith("/") ? jiraInstanceUrl : jiraInstanceUrl + "/";
        userSearchUrl += "rest/api/3/user/search?query=" + reporterIdentifier; // URL 인코딩 필요할 수 있음

        HttpHeaders headers = new HttpHeaders();
        String auth = authUsername + ":" + authToken;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(userSearchUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode usersNode = objectMapper.readTree(response.getBody());
                if (usersNode.isArray() && !usersNode.isEmpty()) {
                    JsonNode firstUser = usersNode.get(0);
                    if (firstUser.has("accountId")) {
                        String accountId = firstUser.get("accountId").asText();
                        return accountId;
                    }
                } else {
                    log.warn("No user found for identifier: {}", reporterIdentifier);
                }
            } else {
                log.error("Failed to fetch user from Jira. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            }
        } catch (HttpClientErrorException e) {
            log.error("Jira API Client Error (user search): {} - {}, Response Body: {}", e.getStatusCode(), e.getMessage(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Error fetching accountId for reporter: {}. Error: {}", reporterIdentifier, e.getMessage(), e);
        }
        return null; // 조회 실패 시
    }

    /**
     * Jira API 에러 응답 본문에서 주요 메시지를 추출합니다.
     * @param responseBody 에러 응답 본문 문자열
     * @param statusCode HTTP 상태 코드
     * @return 추출된 에러 메시지 또는 기본 메시지
     */
    private String extractJiraErrorMessage(String responseBody, int statusCode) {
        if (responseBody == null || responseBody.isBlank()) {
            return "Jira API 오류 (응답 없음, 코드: " + statusCode + ")";
        }
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            if (rootNode.has("errorMessages")) {
                JsonNode errorMessages = rootNode.get("errorMessages");
                if (errorMessages.isArray() && !errorMessages.isEmpty()) {
                    return errorMessages.get(0).asText("알 수 없는 Jira 오류 (코드: " + statusCode + ")");
                }
            }
            // 다른 에러 형식 (필드별 에러 등)
            if (rootNode.has("errors")) {
                JsonNode errorsNode = rootNode.get("errors");
                if (errorsNode.isObject() && errorsNode.fields().hasNext()) {
                    // 첫 번째 필드 에러 메시지만 간단히 반환
                    Map.Entry<String, JsonNode> firstErrorField = errorsNode.fields().next();
                    return "필드 '" + firstErrorField.getKey() + "': " + firstErrorField.getValue().asText() + " (코드: " + statusCode + ")";
                }
            }
            // 때로는 최상위에 "message" 필드가 있을 수 있음
            if (rootNode.has("message") && rootNode.get("message").isTextual()) {
                return rootNode.get("message").asText();
            }

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse Jira error response JSON: {}", responseBody, e);
        }
        // 파싱 실패 또는 특정 형식 아닐 시, 응답 본문 일부 또는 기본 메시지 반환
        return "Jira API 오류 (코드: " + statusCode + ")" + (responseBody.length() > 100 ? responseBody.substring(0,100) : responseBody) ;
    }
}
