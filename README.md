# CHOLOG - Spring Boot 통합 로깅 SDK

CHOLOG는 Spring Boot 애플리케이션을 위한 지능형 로깅 SDK입니다. 최소한의 설정으로 HTTP 요청/응답, 애플리케이션 로그, 처리되지 않은 예외 등을 자동으로 캡처하여 중앙 로그 수집 서버(예: ELK 스택)로 안전하고 효율적으로 전송합니다.

## 주요 특징

- **제로 설정에 가까운 자동 로깅**: HTTP 요청(메소드, URI, 헤더, 파라미터, 본문), 응답(상태 코드, 헤더, 본문), 처리 시간 등을 자동으로 로깅합니다.
- **중앙 집중식 로그 관리**: 모든 로그를 지정된 중앙 로그 서버로 전송하여 통합 관리를 지원합니다.
- **ELK 스택 호환**: 로그를 JSON 형식으로 전송하여 Logstash, Elasticsearch, Kibana (ELK) 스택과 쉽게 연동할 수 있습니다.
- **서비스 식별 및 다중 사용자 지원**: 각 애플리케이션(서비스)은 고유한 API 키, 서비스 이름, 환경 정보를 설정하여 로그를 식별합니다. 이를 통해 여러 서비스/사용자가 동일한 중앙 로그 서버를 사용하면서도 각자의 로그를 명확히 구분할 수 있습니다.
- **고유 요청 ID (Trace ID)**: 각 HTTP 요청에 고유한 UUID를 부여하여, 분산 환경에서도 특정 요청과 관련된 모든 로그를 쉽게 추적합니다.
- **자동 예외 캡처 및 로깅**: 처리되지 않은 모든 예외를 감지하여 상세 정보(스택 트레이스 포함)와 함께 고유 에러 ID를 부여하여 로깅합니다.
- **비동기 및 배치 전송**: 로그 전송으로 인한 애플리케이션 성능 영향을 최소화하기 위해 비동기 방식과 배치 처리를 사용합니다.
- **네트워크 장애 대비**: 로그 전송 실패 시 재시도 로직 및 디스크 큐(Disk Queue) 기능으로 로그 유실을 방지합니다.
- **로그 압축 기능**: 대용량 로그 데이터의 효율적인 전송을 위한 GZIP 압축 지원으로 네트워크 대역폭 사용량을 절감합니다.
- **상태 지표 및 모니터링**: 로그 전송 성공률, 큐 상태, 디스크 사용량 등의 운영 지표를 JMX를 통해 노출하여 모니터링을 용이하게 합니다.
- **민감 정보 자동 필터링**: 설정된 패턴에 따라 로그에 포함된 민감 정보(예: 비밀번호, API 키)를 자동으로 마스킹합니다.
- **유연한 설정**: `application.properties` 또는 `application.yml`을 통해 다양한 로깅 동작을 상세하게 제어할 수 있습니다.

## 최신 버전 정보 (v1.8.7)

* **GZIP 압축 기본 활성화**: 네트워크 대역폭 절약을 위해 GZIP 압축을 기본 활성화함
* **JMX 모니터링 강화**: 로그 전송 상태 모니터링을 위한 JMX 메트릭 확장
  - 큐 크기, 디스크 큐 상태, 서버 연결 상태 등 실시간 모니터링 가능

전체 버전 기록은 [CHANGELOG.md](CHANGELOG.md)를 참조하세요.

## 시작하기

### 1. 의존성 추가

귀하의 Spring Boot 프로젝트의 `build.gradle` 파일에 다음 의존성을 추가합니다:

```gradle
repositories {
	mavenCentral()
	maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.ssafy.lab.eddy1219:chologger:v1.8.7'
    // 기타 의존성...
}
```

(Maven 사용 시 대응하는 `<dependency>` 태그를 추가하세요.)

### 2. 기본 설정 (application.yml)

`src/main/resources/application.yml` (또는 `.properties`) 파일에 설정을 추가합니다.

#### 필수 설정

아래 필수 설정 항목은 반드시 지정해야 합니다:

```yaml
cholog:
  logger:
    # 중앙 로그 서버 URL (필수)
    url: http://your-log-server.com/api/logs
    # API 키 (필수) - 로그 JSON에서 'serverId' 필드로 출력됨
    api-key: your-api-key
    # 서비스 식별 이름 (필수) - 로그 JSON에서 'serviceName' 필드로 출력됨
    service-name: my-awesome-service
    # 환경 설정 (선택) - 기본값은 "development" - 환경별 로그 확인
    environment: production
```

#### 선택적 설정

다음 설정은 선택적이며, 필요에 따라 추가할 수 있습니다. 모든 가능한 설정 옵션과 기본값은 다음과 같습니다:

```yaml
cholog:
  logger:
    # 서비스 정보 설정
    validate-api-key: true                  # API 키 검증 활성화 여부
    
    # 로그 레벨 설정
    log-level: INFO                         # 전송할 최소 로그 레벨 (TRACE, DEBUG, INFO, WARN, ERROR)
    
    # 민감 정보 필터링 설정
    sensitive-patterns:                     # 민감 정보로 간주하여 필터링할 필드 패턴 목록 (기본값: 빈 목록)
      - "password"
      - "card"
    sensitive-value-replacement: "***"      # 민감 정보 대체 문자열
    
    # 배치 처리 관련 설정
    batch-size: 100                         # 한 번에 전송할 로그 최대 개수
    batch-flush-interval: 1000              # 로그 모으는 최대 시간 간격(ms)
    queue-capacity: 10000                   # 메모리 큐 최대 용량
    
    # 재시도 및 네트워크 설정
    max-retries: 3                          # 전송 실패 시 최대 재시도 횟수
    retry-delay: 1000                       # 재시도 간격(ms)
    use-https: false                        # HTTPS 사용 여부
    allow-insecure-tls: false               # TLS 인증서 검증 무시 여부 (개발 환경에서만 사용)
    
    # 연결 오류 로그 최적화 설정 (v1.7.5)
    suppress-connection-errors: true        # 연결 오류 로그 최소화 (상태 변경 시에만 출력)
    max-connection-error-logs-per-period: 1 # 기간 내 최대 연결 오류 로그 수
    connection-error-log-period: 300000     # 오류 로그 제한 적용 주기(ms)
    use-exponential-backoff: true           # 지수 백오프 전략 사용 여부
    initial-backoff-delay: 5000             # 초기 지연 시간(ms)
    max-backoff-delay: 1800000              # 최대 지연 시간(ms)
    verbose-disk-queue-logs: false          # 디스크 큐 관련 상세 로그 출력 여부
    
    # 디스크 큐 설정
    disk-queue-enabled: true                # 디스크 큐 활성화 여부
    disk-queue-path: ./log-queue            # 디스크 큐 저장 경로
    disk-resend-interval: 60000             # 디스크 큐 재전송 간격(ms)
    max-disk-queue-size-mb: 1024            # 디스크 큐 최대 크기(MB)
    
    # 연결 모니터링 설정
    connection-check-interval: 300000       # 서버 연결 상태 확인 간격(ms)
    connection-check-timeout: 5000          # 연결 확인 요청 타임아웃(ms)
    
    # HTTP 클라이언트 풀 설정
    http-client-pool-max-total: 100         # HTTP 클라이언트 풀 최대 연결 수
    http-client-pool-default-max-per-route: 20  # 경로당 최대 연결 수
    http-client-pool-evict-idle-connections-after: 30  # 유휴 연결 제거 시간(초)
    
    # 압축 및 지표 설정
    gzip-enabled: true                      # 로그 압축 활성화 여부
    compression-threshold: 1024             # 압축 시작 최소 크기(바이트)
    metrics-enabled: true                   # 지표 수집 활성화 여부
    metrics-collection-interval: 60000      # 지표 수집 간격(ms)
    expose-metrics-via-jmx: true            # JMX를 통한 지표 노출 여부
    
    # 기본 CORS 설정
    cors-enabled: false                     # 기본 CORS 설정 활성화 여부 (true로 설정 시 모든 오리진/헤더/메소드 허용)
```

> **로그 압축 관련 주의사항**: 로그 압축 기능(`gzip-enabled: true`)을 활성화하는 경우, ELK 스택의 Logstash 설정에 반드시 `decompress_request => true` 옵션을 추가해야 합니다. 그렇지 않으면 압축된 로그 데이터가 제대로 처리되지 않습니다.

**주요 설정 설명:**

-   `cholog.logger.url`: **필수.** 로그를 수신할 중앙 서버의 HTTP 엔드포인트입니다. ELK 스택 사용 시 Logstash의 HTTP 입력 플러그인 URL을 지정합니다.
-   `cholog.logger.api-key`: **필수.** 각 서비스(애플리케이션 인스턴스)를 식별하는 고유한 API 키입니다. 중앙 로그 서버에서 이 키를 사용하여 로그를 필터링하거나 접근 제어를 할 수 있습니다. 로그의 serverId 필드로 저장됩니다.
-   `cholog.logger.service-name`: **필수.** 서비스의 논리적 이름입니다. 로그 검색 및 대시보드 구성에 유용합니다. 로그의 `serviceName` 필드로 출력됩니다.
-   `cholog.logger.environment`: 서비스가 실행되는 환경 (예: `development`, `staging`, `production`).
-   `cholog.logger.cors-enabled`: 기본 CORS 설정을 활성화합니다. true로 설정 시 모든 오리진/헤더/메소드를 허용하는 CorsFilter 빈이 등록됩니다 (v1.8.6).
-   `cholog.logger.suppress-connection-errors`: 연결 오류 로그를 최소화하여 로그 가독성을 높입니다 (v1.7.5).
-   `cholog.logger.use-exponential-backoff`: 서버 연결 문제 시 재시도 간격을 점진적으로 늘립니다 (v1.7.5).

### 3. 애플리케이션에서 로그 사용

CHOLOG SDK는 SLF4J API 위에 구축되어 있으므로, 기존과 동일한 방식으로 로그를 작성하면 됩니다. 별도의 SDK API 호출은 필요 없습니다.

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Service
class MyExampleService {
    private static final Logger log = LoggerFactory.getLogger(MyExampleService.class);

    public String processUserData(String userId, Map<String, String> userData) {
        // 사용자 정의 컨텍스트 정보 추가 (선택 사항)
        try (MDC.MDCCloseable ignored = MDC.putCloseable("custom.userId", userId)) {
            log.info("사용자 데이터 처리 시작: {}", userData.get("action"));
            // ... 비즈니스 로직 ...
            if (userData.containsKey("errorTest")) {
                throw new IllegalArgumentException("테스트 예외 발생!");
            }
            log.debug("사용자 데이터 처리 중 세부 정보: {}", userData);
            return "처리 완료: " + userId;
        } catch (Exception e) {
            // 예외 발생 시 자동으로 GlobalExceptionHandler에 의해 처리 및 로깅되지만,
            // 필요에 따라 여기서 특정 처리를 추가할 수 있습니다.
            log.error("사용자 데이터 처리 중 특정 오류 발생: {}", e.getMessage(), e); 
            throw e; // 예외를 다시 던져 전역 핸들러가 처리하도록 함
        }
    }
}

@RestController
class MyExampleController {
    private static final Logger log = LoggerFactory.getLogger(MyExampleController.class);
    private final MyExampleService myExampleService;

    public MyExampleController(MyExampleService myExampleService) {
        this.myExampleService = myExampleService;
    }

    @GetMapping("/api/users/process")
    public String handleUserProcess(@RequestParam String id, @RequestParam String action) {
        // 이 요청은 자동으로 CHOLOG에 의해 로깅됩니다 (경로, 파라미터, 헤더 등).
        log.info("컨트롤러 레벨 로그: 사용자 처리 요청 수신 - ID: {}", id); 
        Map<String, String> data = new HashMap<>();
        data.put("action", action);
        return myExampleService.processUserData(id, data);
    }
}
```

## ELK 스택 연동 가이드

CHOLOG SDK는 생성된 모든 로그를 JSON 형식으로 중앙 서버 URL (`cholog.logger.url`)로 전송합니다. ELK 스택과 연동하려면 Logstash에 HTTP 입력 플러그인을 설정합니다.

**1. Logstash 파이프라인 설정 (`logstash-cholog.conf` 예시):**

```conf
input {
  http {
    port => 5000 # cholog.logger.url에 설정된 포트와 일치해야 함
    codec => json # CHOLOG는 JSON 형식으로 로그를 전송
    # 압축 로그 처리를 위한 필수 설정 (v1.6.4 이상)
    decompress_request => true # Content-Encoding: gzip 헤더가 있는 경우 자동으로 압축 해제
    # 필요한 경우, API 키 검증 등 추가 처리 (예: headers => { "X-API-Key" => "expected-key" })
    # 여러 서비스의 API 키를 검증하려면 Logstash 필터 플러그인에서 처리하는 것이 더 유연합니다.
  }
}

filter {
  # 수신된 JSON 로그에 대한 추가 처리 (필요한 경우)
  # 예: 타임스탬프 필드 파싱, 필드 이름 변경, GeoIP 정보 추가 등
  date {
    match => [ "timestamp", "ISO8601" ] # CHOLOG 로그의 timestamp 필드 형식에 맞게 설정
  }
  
  # X-Service-Name, X-Environment, X-API-Key 헤더 값을 필드로 추가 (Logstash HTTP input이 헤더를 필드로 추가하는 방식에 따라 다를 수 있음)
  # Http input 플러그인 설정에서 headers_target => "http_headers" 와 같이 설정했다면:
  if [http_headers][X-Service-Name] {
    mutate {
      add_field => { "service_name" => "%{http_headers[X-Service-Name]}" }
    }
  }
  if [http_headers][X-Environment] {
    mutate {
      add_field => { "environment" => "%{http_headers[X-Environment]}" }
    }
  }
  # API 키 자체를 로그에 저장하는 것은 보안상 권장되지 않으므로, 인증 목적으로만 사용하고 로그에서는 제거하는 것이 좋습니다.
}

output {
  elasticsearch {
    hosts => ["http://your-elasticsearch-host:9200"]
    index => "cholog-%{service_name}-%{+YYYY.MM.dd}" # 서비스 이름과 날짜 기반으로 인덱스 생성
    # user => "elastic"
    # password => "your_elastic_password"
  }
  # 디버깅을 위해 콘솔 출력 추가 (선택 사항)
  # stdout { codec => rubydebug }
}
```

> **중요**: 로그 압축 기능을 사용하는 경우(`gzip-enabled: true`), 반드시 Logstash HTTP input 플러그인에 `decompress_request => true` 설정을 추가해야 합니다. 이 설정이 없으면 압축된 로그 데이터가 제대로 처리되지 않습니다.

**2. Logstash 실행:**

```bash
./bin/logstash -f logstash-cholog.conf
```

**3. Kibana에서 로그 확인:**

Kibana 대시보드에서 `cholog-*` 인덱스 패턴을 생성하여 수집된 로그를 검색, 시각화하고 분석할 수 있습니다. `service_name`, `environment`, `requestId` (trace ID), `errorId` 등의 필드를 활용하여 강력한 분석이 가능합니다.

## 최적화된 로그 형식

제공하는 최적화된 로그 구조는 다음과 같습니다:

```json
{
  "timestamp": "2025-05-07T07:02:50.636Z",
  "level": "INFO",
  "message": "Request Finished: GET /api/ok status=200 duration=129ms requestId=847c88f8-bd24-42d6-9b9e-bfa44832ff28",
  "logger": "com.cholog.logger.filter.RequestTimingFilter",
  "thread": "http-nio-8081-exec-1",
  "serviceName": "my-awesome-service",
  "environment": "development",
  "profiles": "dev,test",
  "version": "1.0.0",
  "hostName": "DESKTOP-4QCTMKG",
  "serverId": "123-api-key",
  "requestId": "847c88f8-bd24-42d6-9b9e-bfa44832ff28",
  "requestMethod": "GET",
  "requestUri": "/api/ok",
  "clientIp": "0:0:0:0:0:0:0:1",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36",
  "httpStatus": 200,
  "uaMobile": false,
  "uaPlatform": "Windows",
  "performanceMetrics": {
    "activeThreads": 30,
    "totalThreads": 38,
    "cpuUsage": 1,
    "memoryUsage": 40,
    "responseTime": 129
  },
  "headers": {
    "sec-fetch-mode": "navigate",
    "sec-fetch-site": "none",
    "accept-language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
    "sec-fetch-user": "?1",
    "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
    "sec-ch-ua": "\"Google Chrome\";v=\"135\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"",
    "sec-ch-ua-mobile": "?0",
    "sec-ch-ua-platform": "\"Windows\"",
    "host": "localhost:8081",
    "upgrade-insecure-requests": "1",
    "connection": "keep-alive",
    "cache-control": "max-age=0",
    "accept-encoding": "gzip, deflate, br, zstd",
    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36",
    "sec-fetch-dest": "document"
  },
  "responseHeaders": {
    "Vary": "Origin",
    "Content-Type": "application/json",
    "Transfer-Encoding": "chunked"
  },
  "mdcContext": {
    "custom": "사용자 정의 MDC 값"
  }
}
```

## 고급 설정 및 커스터마이징

### 필터 순서 커스터마이징

CHOLOG가 등록하는 서블릿 필터들(`RequestTimingFilter`, `RequestBodyLoggingFilter`, `RequestResponseLoggingFilter`)의 실행 순서를 변경하거나, 기존 필터들과의 순서를 조정해야 하는 경우 Spring Boot의 표준 방식을 사용하여 `FilterRegistrationBean`을 직접 정의할 수 있습니다. 이 경우, `cholog.logger` 관련 프로퍼티로 자동 등록되는 빈보다 우선적으로 적용됩니다.

### 사용자 정의 로그 필드 추가 (MDC 활용)

SLF4J의 MDC(Mapped Diagnostic Context)를 사용하여 현재 스레드 컨텍스트에 특정 정보를 추가하면, CHOLOG는 해당 정보를 자동으로 수집하여 로그에 포함시킵니다. 이는 특정 비즈니스 ID, 사용자 ID 등을 로그에 남길 때 유용합니다.

```java
import org.slf4j.MDC;

// ...
try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable("custom.userId", userId)) {
    log.info("주문 처리 중...");
    // 이 블록 내에서 발생하는 모든 로그에는 "custom.userId" 정보가 포함됩니다.
}
```

### 프론트엔드와 백엔드 간 요청 추적 (X-Request-Id)

CHOLOG v1.8.6부터는 프론트엔드에서 전달된 `X-Request-Id` 헤더가 있는 경우 이를 감지하여 로그의 requestId로 사용합니다. 이를 통해 프론트엔드와 백엔드 간, 또는 마이크로서비스 간 요청 흐름을 추적할 수 있습니다.

**프론트엔드에서 X-Request-Id 설정 예시 (JavaScript):**

```javascript
// Axios를 사용하는 경우
import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';

// 요청 인터셉터 설정
axios.interceptors.request.use(config => {
  // 모든 요청에 X-Request-Id 헤더 추가
  config.headers['X-Request-Id'] = uuidv4();
  return config;
});

// 또는 개별 요청에 헤더 추가
const requestId = uuidv4();
axios.get('/api/data', {
  headers: {
    'X-Request-Id': requestId
  }
})
.then(response => console.log(`요청 ID: ${requestId}에 대한 응답:`, response.data));
```

**프론트엔드에서 X-Request-Id 설정 예시 (React):**

```javascript
import { createContext, useContext, useState } from 'react';
import { v4 as uuidv4 } from 'uuid';

// 요청 컨텍스트 생성
const RequestContext = createContext();

export function RequestProvider({ children }) {
  const [requestId] = useState(uuidv4());
  
  return (
    <RequestContext.Provider value={{ requestId }}>
      {children}
    </RequestContext.Provider>
  );
}

// 요청 ID를 사용하는 커스텀 훅
export function useFetch() {
  const { requestId } = useContext(RequestContext);
  
  return async (url, options = {}) => {
    const headers = {
      ...(options.headers || {}),
      'X-Request-Id': requestId
    };
    
    const response = await fetch(url, {
      ...options,
      headers
    });
    
    return response;
  };
}
```

CHOLOG는 프론트엔드에서 전달된 X-Request-Id를 자동으로 감지하여 로그에 사용합니다. 별도의 설정 없이 이 기능이 기본적으로 활성화되어 있으므로, 프론트엔드 애플리케이션에서 헤더만 올바르게 설정하면 됩니다.

## 문제 해결 (Troubleshooting)

-   **로그가 전송되지 않는 경우**:
    -   `cholog.logger.url` 설정이 올바른지, 해당 URL로 네트워크 연결이 가능한지 확인합니다.
    -   Logstash 또는 중앙 로그 서버의 입력 플러그인이 정상적으로 실행 중이고 JSON 요청을 수신 대기하고 있는지 확인합니다.
    -   애플리케이션 로그에서 CHOLOG SDK 관련 에러 메시지가 없는지 확인합니다 (특히 `LogSenderService` 관련).

-   **HTTP 요청/응답 본문이 로깅되지 않는 경우**:
    -   `cholog.logger.request-body-logging` 및 `cholog.logger.request-response-logging` 프로퍼티가 `true`로 설정되어 있는지 확인합니다.
    -   요청의 `Content-Type`이 로깅 대상(json, xml, text, form)인지 확인합니다.

-   **민감 정보가 필터링되지 않는 경우**:
    -   `cholog.logger.sensitive-patterns` 설정이 올바른지, 필터링하려는 키와 패턴이 일치하는지 확인합니다.

-   **디스크 큐 저장 오류가 발생하는 경우**:
    -   디스크 큐 디렉토리의 쓰기 권한이 있는지 확인합니다.
    -   디스크 큐 경로가 유효한지 확인합니다.
    -   최대 디스크 큐 크기 설정이 적절한지 확인합니다.

-   **압축된 로그가 ELK에서 제대로 처리되지 않는 경우**:
    -   Logstash HTTP Input 플러그인에 `decompress_request => true` 설정이 추가되어 있는지 확인합니다.
    -   압축 기능이 활성화된 CHOLOG는 `Content-Encoding: gzip` 헤더와 함께 압축된 데이터를 전송합니다. Logstash에서 이 헤더를 인식하고 자동으로 압축을 해제하려면 위 설정이 필수입니다.
    -   로그가 압축되어 전송되는지 네트워크 모니터링 도구(Wireshark, Charles 등)로 확인할 수 있습니다. 압축된 경우 HTTP 헤더에 `Content-Encoding: gzip`이 포함됩니다.
    -   Logstash 로그에서 "Unable to handle Content-Encoding: gzip" 관련 오류가 있는지 확인하세요.

## 커스텀 로그 서버 구현

CHOLOG SDK는 기본적으로 ELK 스택과의 연동을 목표로 설계되었지만, 자체 로그 서버를 구현해 사용할 수도 있습니다. 특히 로그 압축 기능을 사용할 경우 아래 내용을 참고하세요.

### Spring Boot 기반 로그 서버에서 압축 처리

CHOLOG에서 로그 압축 기능(`gzip-enabled: true`)을 활성화했다면, 커스텀 로그 서버에서도 압축된 요청을 처리할 수 있어야 합니다.

**Spring Boot 서버 설정에 압축 해제 기능 추가 (권장)**

`application.properties` 또는 `application.yml` 파일에 다음 설정을 추가합니다:

```yaml
server:
  compression:
    enabled: true
    mime-types: application/json
```

이 설정으로 Spring Boot는 `Content-Encoding: gzip` 헤더가 있는 요청을 자동으로 해제합니다.

**Controller에서 직접 구현하는 방법 (대안)**

컨트롤러에서 직접 압축 해제 로직을 구현하는 방법도 있지만, Spring의 필터 메커니즘을 활용하는 것이 더 권장됩니다:

```java
@Bean
public FilterRegistrationBean<Filter> decompressFilter() {
    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new ContentEncodingFilter());
    registrationBean.addUrlPatterns("/logs/*");
    registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registrationBean;
}
```

### 로그 구조 매핑

커스텀 로그 서버에서 이 구조를 매핑할 때는 다음 필드들을 고려하세요:

- 기본 로그 정보: `level`, `message`, `timestamp`, `logger`, `thread`
- 애플리케이션 정보: `serviceName`, `environment`, `version`, `hostName`, `serverId`
- 요청 정보: `requestId`, `requestMethod`, `requestUri`, `clientIp`, `userAgent`, `httpStatus`
- 브라우저 정보: `uaMobile`, `uaPlatform`
- 구조화된 객체: `performanceMetrics`, `mdcContext`, `headers`, `throwable`

## 상태 지표 모니터링

CHOLOG는 JMX를 통해 로그 전송 상태와 성능 지표를 모니터링할 수 있습니다.

```yaml
cholog:
  logger:
    expose-metrics-via-jmx: true  # 기본값은 true - JMX를 통한 지표 노출
```

JConsole이나 다른 JMX 클라이언트를 통해 다음 경로에서 모니터링 지표를 확인할 수 있습니다:
`com.cholog.logger:type=LoggingMetrics`

주요 지표:
- 로그 큐 크기 및 상태
- 서버 연결 상태
- 디스크 큐 크기 및 파일 개수
- 전송 성공/실패율
- 네트워크 대역폭 사용량

JConsole 또는 VisualVM과 같은 JMX 클라이언트로 `com.cholog.logger:type=LoggingMetrics` MBean에 접근하여 확인할 수 있습니다.

## 라이선스

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

이 SDK는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참고하세요.