# Playwright v1.51.0 Java 이미지를 베이스로 사용 (Ubuntu 22.04 Jammy 기반)
# 이 이미지는 OpenJDK, Playwright 브라우저들, 모든 시스템 의존성을 포함합니다.
# Spring Boot 프로젝트의 Playwright Java 라이브러리 버전도 1.51.0으로 맞춰주세요.
FROM https://www.google.com/search?q=mcr.microsoft.com/playwright/java:v1.51.0-jammy

# (선택 사항) 애플리케이션 작업 디렉토리 설정
WORKDIR /app

# (선택 사항) 한글 폰트가 기본 이미지에 충분하지 않다고 판단되면 추가 설치
# Playwright 기본 이미지에도 다양한 폰트가 포함되어 있을 수 있으므로, 먼저 테스트 후 필요시 추가하세요.
# RUN apt-get update && \
#     apt-get install -y --no-install-recommends \
#     fonts-nanum \
#     && rm -rf /var/lib/apt/lists/*

# Dockerfile 내에서 사용할 변수 JAR_FILE을 정의 (이전과 동일)
ARG JAR_FILE=*.jar

# JAR_FILE 경로에 해당하는 파일을 Docker 이미지 내부로 복사 (이전과 동일)
COPY ${JAR_FILE} spring-prod.jar

# Docker 컨테이너가 시작될 때 실행할 명령을 지정 (이전과 동일)
# (베이스 이미지에 포함된 Java를 사용합니다)
ENTRYPOINT ["java", "-jar", "/spring-prod.jar"]

# (선택 사항) Spring Boot 애플리케이션 포트 노출 (docker-compose.yml에서도 설정 가능)
# EXPOSE 8080