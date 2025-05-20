# Java 17 베이스 이미지 사용
FROM openjdk:17-jdk-slim

# 시스템 패키지 목록 업데이트 및 Playwright 실행에 필요한 의존성 라이브러리 설치
RUN apt-get update -qq
RUN apt-get install -y libgobject-2.0-0
RUN apt-get install -y libgio-2.0-0

# Dockerfile 내에서 사용할 변수 JAR_FILE을 정의
ARG JAR_FILE=*.jar

# JAR_FILE 경로에 해당하는 파일을 Docker 이미지 내부로 복사
COPY ${JAR_FILE} spring-prod.jar

# Docker 컨테이너가 시작될 때 실행할 명령을 지정
ENTRYPOINT ["java", "-jar", "/spring-prod.jar"]