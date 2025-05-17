package com.ssafy.cholog.global.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "요청한 리소스를 찾을 수 없습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다"),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "C004", "로그인이 필요한 서비스입니다"),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "C005", "접근 권한이 없습니다"),

    // Auth
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 액세스 토큰입니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "A002", "리프레시 토큰이 존재하지 않습니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않은 리프레시 토큰입니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U002", "이미 사용 중인 이메일입니다"),
    NICKNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U003", "이미 사용 중인 닉네임입니다"),
    OAUTH_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "U004", "OAuth 서버와 통신 중 오류가 발생했습니다"),
    INVALID_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "U005", "지원하지 않는 OAuth 제공자입니다"),
    NOT_GENERAL_USER(HttpStatus.BAD_REQUEST, "U006", "소셜 로그인으로 가입된 이메일 입니다. 소셜로그인을 시도하세요."),
    NOT_OAUTH_USER(HttpStatus.BAD_REQUEST, "U007", "일반 로그인으로 가입된 이메일 입니다. 일반 로그인으로 로그인하세요."),

    // Project
    PROJECT_USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "P001", "프로젝트 사용자 정보를 찾을 수 없습니다"),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "P002", "프로젝트를 찾을 수 없습니다."),
    PROJECT_ALREADY_JOINED(HttpStatus.BAD_REQUEST, "P003", "이미 참여한 프로젝트입니다."),
    NOT_PROJECT_USER(HttpStatus.NOT_FOUND, "P004", "프로젝트에 참여한 유저가 아닙니다."),
    CREATOR_CANNOT_WITHDRAW(HttpStatus.FORBIDDEN, "P005", "프로젝트 생성자는 탈퇴할 수 없습니다."),

    // Log
    LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "L001", "로그를 찾을 수 없습니다"),
    INDEX_CREATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "L002", "인덱스 생성에 실패했습니다."),
    LOG_START_TIME_AFTER_END_TIME(HttpStatus.BAD_REQUEST, "L003", "시작시간이 종료시간보다 늦을 수 없습니다"),

    // Webhook
    WEBHOOK_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "W001", "이미 웹훅 설정이 존재합니다."),
    WEBHOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "W002", "웹훅 설정을 찾을 수 없습니다."),

    // JIRA
    JIRA_PROJECT_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "J001", "이미 프로젝트의 JIRA 설정이 존재합니다."),
    JIRA_PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "J002", "프로젝트의 JIRA 설정을 찾을 수 없습니다."),
    JIRA_USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "J003", "이미 유저의 JIRA 설정이 존재합니다."),
    JIRA_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "J004", "유저의 JIRA 설정을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
