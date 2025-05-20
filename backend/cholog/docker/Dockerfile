# Java 17 베이스 이미지 사용
FROM openjdk:17-jdk-slim

# 시스템 패키지 목록 업데이트 및 Playwright 실행에 필요한 의존성 라이브러리 설치
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    # Playwright 공식 문서 (Java)에서 권장하는 Debian/Ubuntu 의존성 목록:
    libnss3 \
    libnspr4 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libgbm1 \
    libgtk-3-0 \
    libpango-1.0-0 \
    libasound2 \
    libx11-6 \
    libx11-xcb1 \
    libxcb1 \
    libxcomposite1 \
    libxcursor1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxinerama1 \
    libxrandr2 \
    libxrender1 \
    libxtst6 \
    libxshmfence1 \
    libglib2.0-0 \
    libgobject-2.0-0 \
    libdbus-1-3 \
    libgio-2.0-0 \
    libatspi2.0-0 \
    libexpat1 \
    libxkbcommon0 \
    libcairo2 \
    fonts-nanum \
    && rm -rf /var/lib/apt/lists/*

# Dockerfile 내에서 사용할 변수 JAR_FILE을 정의
ARG JAR_FILE=*.jar

# JAR_FILE 경로에 해당하는 파일을 Docker 이미지 내부로 복사
COPY ${JAR_FILE} spring-prod.jar

# Docker 컨테이너가 시작될 때 실행할 명령을 지정
ENTRYPOINT ["java", "-jar", "/spring-prod.jar"]