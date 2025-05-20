# Java 17 베이스 이미지 사용 - Debian Bullseye 일반 버전으로 가정
FROM openjdk:17-jdk-bullseye

# 환경 변수 설정으로 non-interactive 모드 강제
ENV DEBIAN_FRONTEND=noninteractive

# 1. 패키지 목록 업데이트 (자세한 로그 확인을 위해 -qq 옵션 제거)
#    이 단계의 전체 출력을 확인하는 것이 매우 중요합니다.
RUN echo "Starting apt-get update..." && \
    apt-get update && \
    echo "apt-get update finished."

# 2. 문제가 되는 패키지 중 하나만 먼저 설치 시도 (자세한 로그 확인)
RUN echo "Attempting to install libgobject-2.0-0..." && \
    apt-get install -y --no-install-recommends libgobject-2.0-0 && \
    echo "libgobject-2.0-0 installation attempt finished."

# 3. 다른 문제가 되는 패키지 설치 시도 (자세한 로그 확인)
RUN echo "Attempting to install libgio-2.0-0..." && \
    apt-get install -y --no-install-recommends libgio-2.0-0 && \
    echo "libgio-2.0-0 installation attempt finished."

# 4. (위 단계들이 모두 성공하면) 나머지 Playwright 의존성 라이브러리 및 한글 폰트 설치
#    이 단계는 위 2, 3번이 성공적으로 완료되었을 때 주석을 풀고 실행합니다.
#    모든 패키지를 한 번에 설치하는 것이 더 효율적이지만, 디버깅을 위해 분리했습니다.
# RUN echo "Attempting to install remaining dependencies..." && \
#     apt-get install -y --no-install-recommends \
#     libnss3 \
#     libnspr4 \
#     libatk1.0-0 \
#     libatk-bridge2.0-0 \
#     libcups2 \
#     libdrm2 \
#     libgbm1 \
#     libgtk-3-0 \
#     libpango-1.0-0 \
#     libasound2 \
#     libx11-6 \
#     libx11-xcb1 \
#     libxcb1 \
#     libxcomposite1 \
#     libxcursor1 \
#     libxdamage1 \
#     libxext6 \
#     libxfixes3 \
#     libxinerama1 \
#     libxrandr2 \
#     libxrender1 \
#     libxtst6 \
#     libxshmfence1 \
#     libglib2.0-0 \
#     libdbus-1-3 \
#     libatspi2.0-0 \
#     libexpat1 \
#     libxkbcommon0 \
#     libcairo2 \
#     fonts-nanum \
#     ca-certificates \
#     apt-utils \
#     && rm -rf /var/lib/apt/lists/* \
#     && echo "Remaining dependencies installation finished."

ARG JAR_FILE=*.jar
COPY ${JAR_FILE} spring-prod.jar
ENTRYPOINT ["java", "-jar", "/spring-prod.jar"]