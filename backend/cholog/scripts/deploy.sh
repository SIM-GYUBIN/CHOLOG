#!/bin/bash

DOCKER_APP_NAME="spring-cholog"
NGINX_UPSTREAM_CONFIG_HOST_PATH="/data/nginx/current_upstream.conf"


# 실행중인 blue가 있는지 확인
# 프로젝트의 실행 중인 컨테이너를 확인하고, 해당 컨테이너가 실행 중인지 여부를 EXIST_BLUE 변수에 저장
EXIST_BLUE=$(docker-compose -p "${DOCKER_APP_NAME}-blue" -f docker-compose.blue.yml ps | grep -E "Up|running")

# 배포 시작한 날짜와 시간을 기록
echo "배포 시작일자 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)"


# green이 실행중이면 blue up
# EXIST_BLUE 변수가 비어있는지 확인
if [ -z "$EXIST_BLUE" ]; then

  NEW_CONTAINER_NAME="spring-cholog-blue"

  echo "blue 배포 시작 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)"

  # docker-compose.blue.yml 파일을 사용하여 blue 컨테이너를 빌드하고 실행
  docker-compose -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml up -d --build

  # 30초 동안 대기
  sleep 30

  # 새로 올라온 컨테이너 상태 확인 (blue)
  NEW_CONTAINER_STATUS=$(docker-compose -p "${DOCKER_APP_NAME}-blue" -f docker-compose.blue.yml ps | grep -E "Up|running")
  if [ -n "$NEW_CONTAINER_STATUS" ]; then
      echo "blue 컨테이너 실행 확인. green 중단 시작 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)"

      echo "server ${NEW_CONTAINER_NAME}:8080;" > "${NGINX_UPSTREAM_CONFIG_HOST_PATH}"

      echo "Nginx 설정 리로드 중..."
      docker exec nginx nginx -s reload
      sleep 5

      # docker-compose.green.yml 파일을 사용하여 green 컨테이너를 중지
      docker-compose -p ${DOCKER_APP_NAME}-green -f docker-compose.green.yml down
       # 사용하지 않는 이미지 삭제
      docker image prune -af
      echo "green 중단 완료 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)"
  else
      echo "blue 컨테이너 실행 실패. 롤백 필요 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)"
      exit 1
  fi

# blue가 실행중이면 green up
else
  NEW_CONTAINER_NAME="spring-cholog-green"

  echo "green 배포 시작 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)"
  docker-compose -p ${DOCKER_APP_NAME}-green -f docker-compose.green.yml up -d --build

  sleep 30
  # 새로 올라온 컨테이너 상태 확인 (green)
  NEW_CONTAINER_STATUS=$(docker-compose -p "${DOCKER_APP_NAME}-green" -f docker-compose.green.yml ps | grep -E "Up|running")
  if [ -n "$NEW_CONTAINER_STATUS" ]; then
      echo "green 컨테이너 실행 확인. blue 중단 시작 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)"

      echo "server ${NEW_CONTAINER_NAME}:8080;" > "${NGINX_UPSTREAM_CONFIG_HOST_PATH}"

      echo "Nginx 설정 리로드 중..."
      docker exec nginx nginx -s reload
      sleep 5

      docker-compose -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml down
      docker image prune -af
      echo "blue 중단 완료 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)"
  else
      echo "green 컨테이너 실행 실패. 롤백 필요 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)"
      exit 1
  fi
fi
  echo "배포 종료  : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)"

  echo "===================== 배포 완료 ====================="
  echo