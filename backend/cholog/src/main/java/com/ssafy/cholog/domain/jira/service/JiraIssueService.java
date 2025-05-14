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
    private final ObjectMapper objectMapper; // JSON 페이로드 생성용 (선택적)

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

        // 6. Jira API 요청 본문(payload) 생성 (Lombok 빌더 활용)
        JiraIssueFieldsPayload.JiraIssueFieldsPayloadBuilder fieldsBuilder = JiraIssueFieldsPayload.builder()
                .project(JiraIssueIdentifierPayload.builder().key(jiraProjectKey).build())
                .summary(request.getSummary())
                .issuetype(JiraIssueIdentifierPayload.builder().name(request.getIssueType()).build());

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            fieldsBuilder.description(request.getDescription()); // ADF 사용 시 객체로 설정
        }

        // 보고자(Reporter) 설정
        String reporterAccountIdToUse = null;
        if (request.getReporterName() != null && !request.getReporterName().isBlank()) { // 이름/이메일 등으로 조회 시도
            // 여기서 reporterName이 실제로는 이메일일 수 있도록 클라이언트와 약속하거나, DTO 필드를 분리 (reporterEmail)
            reporterAccountIdToUse = fetchAccountIdByReporterIdentifier(jiraInstanceUrl, request.getReporterName(), jiraApiUsername, jiraToken);
            if (reporterAccountIdToUse == null) {
                log.warn("Could not fetch accountId for reporterName: {}. Falling back or issue creation might fail.", request.getReporterName());
                // fallback: API 호출자 본인을 보고자로 지정하거나, 이름으로라도 시도 (이전 문제 발생 가능)
                // 또는 여기서 에러를 발생시켜 이슈 생성을 중단할 수도 있음
            }
        }

        if (reporterAccountIdToUse != null) {
            fieldsBuilder.reporter(JiraIssueIdentifierPayload.builder().accountId(reporterAccountIdToUse).build());
        } else if (jiraApiUsername != null && !jiraApiUsername.isBlank()) {
            // API 호출자 본인을 보고자로 지정 (이 경우에도 API 호출자의 accountId를 미리 저장해두고 사용하는 것이 최선)
            // String apiUserAccountId = jiraUser.getAccountId(); // JiraUser 엔티티에 API 호출자의 accountId가 저장되어 있다면
            // if (apiUserAccountId != null) {
            //    fieldsBuilder.reporter(JiraIssueIdentifierPayload.builder().accountId(apiUserAccountId).build());
            // } else {
            fieldsBuilder.reporter(JiraIssueIdentifierPayload.builder().name(jiraApiUsername).build()); // 이메일로 시도
            log.info("Setting reporter to API user by name/email: {}", jiraApiUsername);
            // }
        } else {
            log.error("Reporter could not be determined. Issue creation will likely fail if reporter is required.");
        }

        // 담당자(Assignee) 설정
        if (request.getAssigneeName() != null && !request.getAssigneeName().isBlank()) {
            fieldsBuilder.assignee(JiraIssueIdentifierPayload.builder().name(request.getAssigneeName()).build());
        }

        // 레이블(Labels) 설정
        if (request.getLabels() != null && !request.getLabels().isEmpty()) {
            fieldsBuilder.labels(request.getLabels());
        }

        JiraIssueFieldsPayload fieldsPayload = fieldsBuilder.build();

        // 사용자 정의 필드(Custom Fields) 설정
        // JiraIssueFieldsPayload 객체 생성 후 addCustomField 메소드 사용
//        if (request.getCustomFields() != null && !request.getCustomFields().isEmpty()) {
//            request.getCustomFields().forEach(fieldsPayload::addCustomField);
//        }

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

        // 7. HTTP 요청 생성 및 전송
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Basic Authentication: Jira 사용자 이름과 API 토큰 사용
        String auth = jiraApiUsername + ":" + jiraToken;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON)); // 응답 타입을 JSON으로 기대

        HttpEntity<String> entity = new HttpEntity<>(jiraPayloadJson, headers);

        // Jira API 엔드포인트 (Jira Cloud API v2 또는 v3, Jira Server 등 확인 필요)
        String jiraApiEndpoint = jiraInstanceUrl.endsWith("/") ? jiraInstanceUrl : jiraInstanceUrl + "/";
        jiraApiEndpoint += "rest/api/2/issue";

        log.info("Sending JIRA Issue Creation Request to: {}", jiraApiEndpoint);
        log.debug("JIRA API Request Payload (JSON): {}", jiraPayloadJson); // 토큰은 실제 로그에 남기지 않도록 주의

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(jiraApiEndpoint, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) { // 일반적으로 201 Created
                log.info("Jira issue created successfully. Status: {}, Response: {}", response.getStatusCode(), response.getBody());
                String responseBody = response.getBody();
                try {
                    JsonNode rootNode = objectMapper.readTree(responseBody);
                    String issueId = rootNode.path("id").asText();
                    String issueKey = rootNode.path("key").asText();
                    String issueApiUrl = rootNode.path("self").asText();

                    // 사용자에게 보여줄 Jira 이슈 URL 생성
                    String userViewableIssueUrl = jiraInstanceUrl.endsWith("/") ? jiraInstanceUrl : jiraInstanceUrl + "/";
                    userViewableIssueUrl += "browse/" + issueKey;

                    return new JiraIssueCreationResponse(issueKey, userViewableIssueUrl, issueApiUrl, issueId);

                } catch (JsonProcessingException e) {
                    log.error("Failed to parse Jira issue creation response: {}", responseBody, e);
                    // 이슈는 생성되었지만, 응답 파싱에 실패한 경우.
                    // 이 경우 어떻게 처리할지 정책이 필요합니다. (예: 기본 응답 반환 또는 에러)
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
        // reporterIdentifier는 이름일 수도, 이메일일 수도 있습니다. 이메일이 더 정확합니다.
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
            log.info("Fetching accountId for reporter identifier: {}", reporterIdentifier);
            ResponseEntity<String> response = restTemplate.exchange(userSearchUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode usersNode = objectMapper.readTree(response.getBody());
                if (usersNode.isArray() && !usersNode.isEmpty()) {
                    if (usersNode.size() > 1) {
                        // 여러 사용자가 검색된 경우의 처리 (예: 첫 번째 사용자를 선택하거나, 이메일로 더 정확히 필터링)
                        log.warn("Multiple users found for identifier: {}. Using the first one.", reporterIdentifier);
                        // TODO: 더 정교한 매칭 로직 필요 (예: 이메일이 일치하는 사용자 찾기)
                    }
                    JsonNode firstUser = usersNode.get(0);
                    if (firstUser.has("accountId")) {
                        String accountId = firstUser.get("accountId").asText();
                        log.info("Found accountId: {} for identifier: {}", accountId, reporterIdentifier);
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
            // Jira Cloud 에러 형식 중 하나
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
