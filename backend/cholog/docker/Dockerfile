# Playwright v1.51.0 Java 이미지를 베이스로 사용 (Ubuntu 22.04 Jammy 기반)
# 이 이미지는 OpenJDK, Playwright 브라우저들, 모든 시스템 의존성을 포함합니다.
FROM mcr.microsoft.com/playwright/java:v1.51.0-jammy

ARG JAR_FILE=*.jar

# JAR_FILE 경로에 해당하는 파일을 Docker 이미지 내부로 복사 (이전과 동일)
COPY ${JAR_FILE} spring-prod.jar

# Docker 컨테이너가 시작될 때 실행할 명령을 지정
ENTRYPOINT ["java", "-jar", "/spring-prod.jar"]
