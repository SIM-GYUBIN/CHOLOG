# 포팅 메뉴얼

# 1.  소스 빌드 및 배포

## 1.1 메인 서비스 서버 (frontend, backend)

### 1.1.1 개발 환경 정보

프로젝트에서 사용된 주요 기술 스택 및 개발 도구의 버전과 설정 정보입니다.

|  | **Skill** | **Version** |
| --- | --- | --- |
| **Back** | **Java** | 17 |
|  | **SpringBoot** | 3.4.3 |
|  | **IntelliJ** | 17.0.12+1-b1087.25 amd64 |
| **Front** | **VS Code** | 1.98.2 |
|  | **Node.js** | 22.15.0 |
|  | **NPM** | 11.0.0 |
| **Data** | **MariaDB** | 11.7.2 |
| **Server** | **AWS EC2** | Ubuntu 22.04.4 LTS |

### 1.1.2 빌드 시 사용되는 환경 변수

빌드 과정에서 참조하는 환경 변수 목록과 설명입니다.

- backend

| 변수명 | 내용 | 비고 |
| --- | --- | --- |
| DB_URL | DB 주소 |  |
| DB_USERNAME | DB 유저네임 |  |
| DB_PASSWORD | DB 비밀번호 |  |
| REDIS_HOST | Redis 호스트 | 사용 X |
| REDIS_PASSWORD | Redis 비밀번호 | 사용 X |
- frontend

| 변수명 | 내용 | 비고 |
| --- | --- | --- |
| VITE_API_BASE_URL | 서비스 도메인 |  |

### 1.1.3 배포 시 특이사항

- nginx 설정

> pdf 생성 시 playwright가 asset에 접근하기 위해 CORS 설정을 해줘야 합니다.
> 

nginx/default.conf

```bash
server {
    listen 443 ssl;
    server_name {YOUR_SERVER_NAME};

    ssl_certificate {}
    ssl_certificate_key {}
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    root {YOUR_ROOT_DIRECTORY};

    # /assets 경로에 대한 CORS 설정 추가 (CSS, JS, 폰트, 이미지 등 정적 에셋)
    location /assets {
        # --- CORS 헤더 추가 ---
        add_header 'Access-Control-Allow-Origin' '*' always;
        add_header 'Access-Control-Allow-Methods' 'GET, HEAD, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'Origin, X-Requested-With, Content-Type, Accept, Range' always;

        # OPTIONS 메서드 (preflight 요청) 처리
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Allow-Origin' '*';
            add_header 'Access-Control-Allow-Methods' 'GET, HEAD, OPTIONS';
            add_header 'Access-Control-Allow-Headers' 'Origin, X-Requested-With, Content-Type, Accept, Range';
            add_header 'Content-Length' 0;
            return 204;
        }
        # --- CORS 헤더 추가 끝 ---

        add_header Cache-Control "public, max-age=31536000, immutable";

        try_files $uri =404;
    }
```

### 1.1.4 주요 설정 파일 목록

프로젝트의 주요 계정 정보, DB 접속 정보, 프로퍼티 등이 정의된 파일 목록입니다.

- backend

| 파일 경로 및 이름 | 주요 내용 | 비고 |
| --- | --- | --- |
| src/main/resources/application-prod.yml | DB 접속 정보 | 환경 변수 포함 파일 |
| src/main/resources/application-secret.yml | elasticsearch 연결 정보, groq api key 등 비밀 정보 |  |
- application-secret.yml

```yaml
spring:
  elasticsearch:
    username: {your_user_name}
    password: {your_password}
    uris: "{your_host:your_port}"

groq:
  default-model: {your_model}
  default-temperature: {your_temperature}
  default-max-tokens: {your_max_tokens}
  api:
    key: {your_api_key}

jwt:
  secret: {your_secret_token}

oauth:
  ssafy:
    client-id: {your_client_id}
    client-secret: {your_client_secret}
```

- frontend

| 파일 경로 및 이름 | 주요 내용 | 비고 |
| --- | --- | --- |
| .env | 서비스 도메인 |  |
- .env

```yaml
VITE_API_BASE_URL={your_domain}
```

## 1.2 Log 수집/저장 서버 (log)

### 1.2.1 개발 환경 정보

프로젝트에서 사용된 주요 기술 스택 및 개발 도구의 버전과 설정 정보입니다.

|  | **Skill** | **Version** |
| --- | --- | --- |
| **Back** | **Java** | 17 |
|  | **SpringBoot** | 3.4.3 |
|  | **IntelliJ** | 17.0.12+1-b1087.25 amd64 |
| **Data** | **Elasticsearch** | 9.0.0 |
|  | **Logstash** | 9.0.0 |
|  | **Kibana** | 9.0.0 |
| **Server** | **AWS EC2** | Ubuntu 24.04.2 LTS |

### 1.2.2 빌드 시 사용되는 환경 변수

-

### 1.2.3 배포 시 특이사항

- docker elk 설치 필요

<aside>

https://github.com/deviantony/docker-elk

깃 클론 후 설정에 맞게 수정

| 파일 경로 및 이름 | 주요 내용 | 비고 |
| --- | --- | --- |
| docker-compose.yml | 환경 설정 |  |
| .env | 환경 변수 |  |
| logstash/config/pipelines.yml | 파이프라인 설정 |  |
| logstash/pipeline/frontend.conf | 프론트엔드 로그수집 파이프라인 |  |
| logstash/pipeline/backend.conf | 백엔드 로그수집 파이프라인 |  |
- docker-compose.yml

```yaml
services:

  setup:
    profiles:
      - setup
    build:
      context: setup/
      args:
        ELASTIC_VERSION: ${ELASTIC_VERSION}
    init: true
    volumes:
      - ./setup/entrypoint.sh:/entrypoint.sh
      - ./setup/lib.sh:/lib.sh:ro,Z
      - ./setup/roles:/roles:ro,Z
    environment:
      ELASTIC_PASSWORD: ${ELASTIC_PASSWORD:-}
      LOGSTASH_INTERNAL_PASSWORD: ${LOGSTASH_INTERNAL_PASSWORD:-}
      KIBANA_SYSTEM_PASSWORD: ${KIBANA_SYSTEM_PASSWORD:-}
      METRICBEAT_INTERNAL_PASSWORD: ${METRICBEAT_INTERNAL_PASSWORD:-}
      FILEBEAT_INTERNAL_PASSWORD: ${FILEBEAT_INTERNAL_PASSWORD:-}
      HEARTBEAT_INTERNAL_PASSWORD: ${HEARTBEAT_INTERNAL_PASSWORD:-}
      MONITORING_INTERNAL_PASSWORD: ${MONITORING_INTERNAL_PASSWORD:-}
      BEATS_SYSTEM_PASSWORD: ${BEATS_SYSTEM_PASSWORD:-}
    networks:
      - elk
    depends_on:
      - elasticsearch

  elasticsearch:
    build:
      context: elasticsearch/
      args:
        ELASTIC_VERSION: ${ELASTIC_VERSION}
    volumes:
      - ./elasticsearch/config/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml:ro,Z
      - elasticsearch:/usr/share/elasticsearch/data:Z
    ports:
      - 9200:9200
      - 9300:9300
    environment:
      node.name: elasticsearch
      ES_JAVA_OPTS: -Xms512m -Xmx512m
      # Bootstrap password.
      # Used to initialize the keystore during the initial startup of
      # Elasticsearch. Ignored on subsequent runs.
      ELASTIC_PASSWORD: ${ELASTIC_PASSWORD:-}
      # Use single node discovery in order to disable production mode and avoid bootstrap checks.
      # see: https://www.elastic.co/docs/deploy-manage/deploy/self-managed/bootstrap-checks
      discovery.type: single-node
    networks:
      - elk
    restart: unless-stopped

  logstash:
    build:
      context: logstash/
      args:
        ELASTIC_VERSION: ${ELASTIC_VERSION}
    volumes:
      - ./logstash/config/logstash.yml:/usr/share/logstash/config/logstash.yml:ro,Z
      - ./logstash/config/pipelines.yml:/usr/share/logstash/config/pipelines.yml:ro,Z 
      - ./logstash/pipeline:/usr/share/logstash/pipeline:ro,Z
    ports:
      - 5044:5044
      - 50000:50000/tcp
      - 51000:51000/tcp
      - 9600:9600
    environment:
      LS_JAVA_OPTS: -Xms256m -Xmx256m
      LOGSTASH_INTERNAL_PASSWORD: ${LOGSTASH_INTERNAL_PASSWORD:-}
    networks:
      - elk
    depends_on:
      - elasticsearch
    restart: unless-stopped

  kibana:
    build:
      context: kibana/
      args:
        ELASTIC_VERSION: ${ELASTIC_VERSION}
    volumes:
      - ./kibana/config/kibana.yml:/usr/share/kibana/config/kibana.yml:ro,Z
    ports:
      - 5601:5601
    environment:
      KIBANA_SYSTEM_PASSWORD: ${KIBANA_SYSTEM_PASSWORD:-}
    networks:
      - elk
    depends_on:
      - elasticsearch
    restart: unless-stopped

networks:
  elk:
    driver: bridge

volumes:
  elasticsearch:

```

- logstash/config/pipelines.yml

```yaml
- pipeline.id: frontend-pipeline
  path.config: "/usr/share/logstash/pipeline/frontend.conf"
- pipeline.id: backend-pipeline
  path.config: "/usr/share/logstash/pipeline/backend.conf"
```

- logstash/pipeline/frontend.conf

```yaml
input {
  beats {
    port => 5044
  }
  http {
    port => 50000
    add_field => { "[@metadata][input_pipeline]" => "http_json_array_processor" }
  }
}
filter {
  if [@metadata][input_pipeline] == "http_json_array_processor" {
    json {
      source => "message"
      target => "TEMP_PARSED_JSON_ARRAY"
    }
    if [TEMP_PARSED_JSON_ARRAY] {
      split {
        field => "TEMP_PARSED_JSON_ARRAY"
      }
      if [TEMP_PARSED_JSON_ARRAY] {
        ruby {
          code => "
            element = event.get('TEMP_PARSED_JSON_ARRAY')
            if element.is_a?(Hash)
              element.each do |key, value|
                event.set(key, value)
              end
            else
              event.tag('_split_element_not_object')
            end
            event.remove('TEMP_PARSED_JSON_ARRAY')
          "
        }
      }
    }
  }
  if "_jsonparsefailure" in [tags] {
    mutate {
      remove_tag => ["_jsonparsefailure"]
    }
  }
}
output {
  elasticsearch {
    hosts => "elasticsearch:9200"
    user => "logstash_internal"
    password => "${LOGSTASH_INTERNAL_PASSWORD}"
    index => "pjt-fe-%{[projectKey]}"
  }
  stdout { codec => rubydebug }
}
```

- logstash/pipeline/backend.conf

```yaml
input {
  http {
    port => 51000
    codec => "json"
  }
}

filter {
   json {
     source => "message"
     target => "TEMP_BE_LOG_ARRAY"
   }
  
   if [TEMP_BE_LOG_ARRAY] {
     split {
       field => "TEMP_BE_LOG_ARRAY"
     }
     if [TEMP_BE_LOG_ARRAY] {
       ruby {
         code => "
           element = event.get('TEMP_BE_LOG_ARRAY')
           if element.is_a?(Hash)
             element.each do |key, value| event.set(key, value) end
           else
             event.tag('_be_split_element_not_object')
           end
           event.remove('TEMP_BE_LOG_ARRAY')
           event.remove('message')
         "
       }
     }
   } else if [message] {
       mutate { add_tag => ["_be_json_parse_failure_or_not_array"] }
   }

}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    user => "logstash_internal"
    password => "${LOGSTASH_INTERNAL_PASSWORD}"
    index => "pjt-be-%{[projectKey]}"
  }
}
```

</aside>

### 1.2.4 주요 설정 파일 목록

-

# 2. 외부 서비스 정보

프로젝트에서 사용하는 외부 서비스의 가입 정보 및 활용에 필요한 설정 정보입니다.

### 2.1. [외부 서비스 1-싸피 로그인]

- **서비스 개요:** 싸피 계정으로 간편 로그인 가능
- 1.1.4 의 application-secret.yml 참고

### 2.2. [외부 서비스 2 Groq- LLM 로그분석]

- **서비스 개요:** LLM으로 로그 분석 가능
- 1.1.4 의 application-secret.yml 참고

# 3. Frontend SDK

### 3.1 Frontend Log를 수신하는 API 변경

`Logger.tsx`의 상단의 아래 코드에 로그 수신 API를 설정하면 됩니다.

```jsx
private static apiEndpoint = "[https://cholog-server.shop/api/logs/js](https://cholog-server.shop/api/logs/js)";
```

### 3.2 NPM Update

1. Package.json 내 변경사항이 있다면 수정합니다.
2. `npm login` 합니다.
    1. npm package의 maintainer로 등록된 계정이어야 합니다.
3. `npm version patch` 를 통해 버전을 업그레이드 합니다.
4. `npm publish` 를 통해 npm에 반영합니다.

# 4. Backend SDK

### 3.1 라이브러리 배포

1. `build.gradle`의 하단의 아래 코드에 url을 설정합니다.
    1. 수정한 내용들을 git repo에 push 한 뒤 tag를 작성합니다.

```java
// JitPack 배포를 위한 Maven 게시 설정
publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java

            // POM 메타데이터 추가
            pom {
                name = 'CHO:LOG Central Logging Library' // 라이브러리 이름
                description = 'A Spring Boot auto-configured library for sending enriched logs to a central CHO:LOG server' // 라이브러리 설명
                url = 'https://lab.ssafy.com/s12-final/S12P31B207'

                licenses {
                    license {
                        name = 'MIT License'
                        url = 'https://opensource.org/licenses/MIT'
                    }
                }

                developers {
                    developer {
                        id = 'eddy1219'
                        name = 'Daehyun'
                        email = 'eddy152264@gmail.com'
                    }
                }
            }
        }
    }
}

```

1. jitPack에 login 합니다.
    1. Settings - Authentication - Git server
    url([https://lab.ssafy.com](https://lab.ssafy.com/)) 및 gitlab에서 발급한 Token을 입력합니다.
2. git repo의 주소를 입력합니다.
3. Get it 버튼을 클릭해 build 합니다.