# CHO:LOG - Spring Boot 통합 로깅 SDK

CHO:LOG는 Spring Boot 애플리케이션을 위한 지능형 로깅 SDK입니다. 최소한의 설정으로 HTTP 요청/응답, 애플리케이션 로그, 처리되지 않은 예외 등을 자동으로 캡처하여 중앙 로그 수집 서버(예: ELK 스택)로 안전하고 효율적으로 전송합니다.

## 주요 특징

- **제로 설정에 가까운 자동 로깅**: HTTP 요청(메소드, URI, 헤더, 파라미터), 응답(상태 코드), 처리 시간 등을 자동으로 로깅합니다. (요청/응답 본문 로깅은 별도 필터에서 처리)
- **중앙 집중식 로그 관리**: 모든 로그를 지정된 중앙 로그 서버로 전송하여 통합 관리를 지원합니다.
- **ELK 스택 호환**: 로그를 JSON 형식으로 전송하여 Logstash, Elasticsearch, Kibana (ELK) 스택과 쉽게 연동할 수 있습니다.
- **서비스 식별 및 다중 사용자 지원**: 각 애플리케이션(서비스)는 고유한 API 키, 서비스 이름, 환경 정보를 설정하여 로그를 식별합니다. 이를 통해 여러 서비스/사용자가 동일한 중앙 로그 서버를 사용하면서도 각자의 로그를 명확히 구분할 수 있습니다.
- **고유 요청 ID (Trace ID)**: 각 HTTP 요청에 고유한 UUID(또는 프론트엔드 제공 ID)를 부여하여, 분산 환경에서도 특정 요청과 관련된 모든 로그를 쉽게 추적합니다.
- **자동 예외 캡처 및 로깅**: 처리되지 않은 모든 예외를 감지하여 상세 정보(스택 트레이스 포함)와 함께 로깅합니다. (`GlobalExceptionHandler` 연계)
- **비동기 및 배치 전송**: 로그 전송으로 인한 애플리케이션 성능 영향을 최소화하기 위해 비동기 방식과 배치 처리를 사용합니다.
- **네트워크 장애 대비**: 로그 전송 실패 시 재시도 로직 및 디스크 큐(Disk Queue) 기능으로 로그 유실을 방지합니다.
- **로그 압축 기능**: 대용량 로그 데이터의 효율적인 전송을 위한 GZIP 압축 지원으로 네트워크 대역폭 사용량을 절감합니다. (서버 설정 필요)
- **상태 지표 및 모니터링**: 로그 전송 성공률, 큐 상태, 디스크 사용량 등의 운영 지표를 JMX를 통해 노출하여 모니터링을 용이하게 합니다.
- **민감 정보 자동 필터링**: `RequestTimingFilter` 및 `LogSenderService`에서 설정된 패턴에 따라 로그에 포함된 민감 정보(예: 비밀번호, API 키 등)를 자동으로 마스킹합니다.
- **유연한 설정**: `application.properties` 또는 `application.yml`을 통해 다양한 로깅 동작을 상세하게 제어할 수 있습니다.

## 최신 버전 정보 (v1.0.6)

* **예외 정보 필드명 수정**:
    - 예외 정보를 담는 필드명을 `throwable`에서 `error`로 변경
    - 필드명 일관성 개선 및 표준화 
    - 모든 오류 로그에서 일관된 필드명 사용으로 검색 용이성 향상
* **HTTP 관련 필드 그룹화 버그 수정**:
    - HTTP 필드가 루트 레벨에 중복해서 남아있는 문제 수정
    - `responseTime` 필드도 루트 레벨에서 제거하도록 로직 추가
    - 일관된 로그 포맷 유지를 위한 코드 개선
* **환경 정보 필드 표준화**:
    - 기존 `environment` 필드를 제거하고 Spring 활성 프로필 정보를 `environment` 필드로 통합
    - `profiles` 필드명을 `environment`로 변경하여 명확성 향상
    - Spring 환경 설정과 로그 필드명 간의 일관성 개선

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
    implementation 'com.ssafy.lab.s12-final:S12P31B207:v1.0.6'
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
    # 중앙 로그 서버 URL (필수, 기본값 CHO:LOG server)
    url: https://cholog-server.shop/api/logs/be # 예시 엔드포인트
    # API 키 (필수) - 로그 JSON에서 'apiKey' 필드로 출력됨
    api-key: your-api-key
```

#### 선택적 설정

다음 설정은 선택적이며, 필요에 따라 추가할 수 있습니다. 모든 가능한 설정 옵션과 기본값은 다음과 같습니다 (주요 설정 위주):

```yaml
cholog:
  logger:
    # 서비스 식별 이름 (권장) - 로그 JSON에서 'serviceName' 필드로 출력됨, default : unknown-service
    service-name: unknown-service
    
    # 로그 레벨 설정
    log-level: INFO                         # 전송할 최소 로그 레벨 (TRACE, DEBUG, INFO, WARN, ERROR) - Appender 필터링 기준
    
    # 민감 정보 필터링 설정 (LogSenderService 및 RequestTimingFilter에서 사용)
    sensitive-patterns:                     # 민감 정보로 간주하여 필터링할 필드 경로 패턴 목록 (예: ["user.password", "creditCard"])
      - "password"
      - "ssn"
      - "token"
    sensitive-value-replacement: "[FILTERED]" # 민감 정보 대체 문자열 (기본값: "***")
    
    # 배치 처리 관련 설정 (LogSenderService)
    batch-size: 100                         # 한 번에 전송할 로그 최대 개수
    batch-flush-interval: 1000              # 로그 모으는 최대 시간 간격(ms)
    queue-capacity: 10000                   # 메모리 큐 최대 용량
    
    # 재시도 및 네트워크 설정 (LogSenderService)
    max-retries: 3                          # 전송 실패 시 최대 재시도 횟수
    retry-delay: 1000                       # 재시도 간격(ms) - 고정 지연 시 사용
    use-https: false                        # HTTPS 사용 여부
    allow-insecure-tls: false               # TLS 인증서 검증 무시 여부 (개발 환경에서만 주의해서 사용)
    
    # 연결 오류 로그 최적화 설정 (LogSenderService)
    suppress-connection-errors: true        # 연결 오류 로그 최소화
    max-connection-error-logs-per-period: 1 
    connection-error-log-period: 300000     
    use-exponential-backoff: true           # 지수 백오프 전략 사용 여부
    initial-backoff-delay: 5000             
    max-backoff-delay: 1800000              
    verbose-disk-queue-logs: false          # 디스크 큐 관련 상세 로그 출력 여부
    
    # 디스크 큐 설정 (LogSenderService)
    disk-queue-enabled: true                # 디스크 큐 활성화 여부
    disk-queue-path: ./log-queue            # 디스크 큐 저장 경로
    disk-resend-interval: 60000             # 디스크 큐 재전송 간격(ms)
    max-disk-queue-size-mb: 1024            # 디스크 큐 최대 크기(mb)
    
    # 연결 모니터링 설정 (LogSenderService)
    connection-check-interval: 300000       # 서버 연결 상태 확인 간격(ms)
    connection-check-timeout: 5000          # 연결 확인 요청 타임아웃(ms)
    
    # HTTP 클라이언트 풀 설정 (LogSenderService)
    http-client-pool-max-total: 100         
    http-client-pool-default-max-per-route: 20  
    http-client-pool-evict-idle-connections-after: 30  
    
    # 압축 및 지표 설정 (LogSenderService)
    compress-logs: true                     # 로그 압축 활성화 여부 (LogServerProperties에는 gzip-enabled)
    metrics-enabled: true                   # 지표 수집 활성화 여부 (LogSenderService 내부 로직)
    metrics-collection-interval: 60000      # 지표 수집 간격(ms)
    expose-metrics-via-jmx: true            # JMX를 통한 지표 노출 여부
    
    # 기본 CORS 설정 (LogAutoConfiguration)
    cors-enabled: false
```

> **로그 압축 관련 주의사항**: 로그 압축 기능(`cholog.logger.compress-logs: true`)을 활성화하는 경우, ELK 스택의 Logstash 설정에 반드시 `decompress_request => true` 옵션을 추가해야 합니다. 그렇지 않으면 압축된 로그 데이터가 제대로 처리되지 않습니다.

**주요 설정 설명:**

-   `cholog.logger.url`: **필수.** 로그를 수신할 중앙 서버의 HTTP 엔드포인트입니다. ELK 스택 사용 시 Logstash의 HTTP 입력 플러그인 URL을 지정합니다.
-   `cholog.logger.api-key`: **필수.** 각 서비스(애플리케이션 인스턴스)를 식별하는 고유한 API 키입니다. 중앙 로그 서버에서 이 키를 사용하여 로그를 필터링하거나 접근 제어를 할 수 있습니다. 로그의 `apiKey` 필드로 저장됩니다.
-   `cholog.logger.service-name`: **필수.** 서비스의 논리적 이름입니다. 로그 검색 및 대시보드 구성에 유용합니다. 로그의 `serviceName` 필드로 출력됩니다.
-   `cholog.logger.environment`: 서비스가 실행되는 환경 (예: `development`, `staging`, `production`).
-   `cholog.logger.sensitive-patterns`: 로그 전송 전 `LogSenderService`에서 필터링할 JSON 필드 경로 패턴 목록입니다. `RequestTimingFilter`는 파라미터 키에 대해 자체 키워드 목록과 이 설정을 함께 고려할 수 있습니다.
-   `cholog.logger.cors-enabled`: 기본 CORS 설정을 활성화합니다. true로 설정 시 모든 오리진/헤더/메소드를 허용하는 `CorsFilter` 빈이 등록될 수 있습니다.

### 3. 애플리케이션에서 로그 사용

CHO:LOG SDK는 SLF4J API 위에 구축되어 있으므로, 기존과 동일한 방식으로 로그를 작성하면 됩니다. 별도의 SDK API 호출은 필요 없습니다.

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC; // MDC 사용 예시
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap; // 예시용
import java.util.Map; // 예시용

@Service
class MyExampleService {
    private static final Logger log = LoggerFactory.getLogger(MyExampleService.class);

    public String processUserData(String userId, Map<String, String> userData) {
        // 사용자 정의 컨텍스트 정보 추가 (선택 사항)
        try (MDC.MDCCloseable ignored = MDC.putCloseable("custom.userId", userId);
             MDC.MDCCloseable ignored2 = MDC.putCloseable("orderId", "ORD987")) { // 여러 MDC 값 추가 가능
            log.info("사용자 데이터 처리 시작: {}", userData.get("action"));
            // ... 비즈니스 로직 ...
            if ("exception".equals(userData.get("action"))) { // "action" 키 존재 확인
                throw new IllegalArgumentException("테스트 예외 발생!");
            }
            log.debug("사용자 데이터 처리 중 세부 정보: {}", userData);
            return "처리 완료: " + userId;
        } catch (Exception e) {
            log.error("사용자 데이터 처리 중 특정 오류 발생: {}", e.getMessage(), e); 
            throw e; 
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
        log.info("컨트롤러 레벨 로그: 사용자 처리 요청 수신 - ID: {}", id); 
        Map<String, String> data = new HashMap<>();
        data.put("action", action);
        // 예시를 위해 파라미터 추가
        // 실제로는 RequestTimingFilter가 request_param_id, request_param_action을 MDC에 추가합니다.
        return myExampleService.processUserData(id, data);
    }
}
```

## ELK 스택 연동 가이드

CHO:LOG SDK는 생성된 모든 로그를 JSON 형식으로 중앙 서버 URL (`cholog.logger.url`)로 전송합니다. ELK 스택과 연동하려면 Logstash에 HTTP 입력 플러그인을 설정합니다.

**1. Logstash 파이프라인 설정 (`logstash-cholog.conf` 예시):**

```conf
input {
  http {
    port => 5000 # cholog.logger.url에 설정된 포트와 일치해야 함
    codec => json_lines # CHO:LOG는 로그 배열을 보내므로, 개별 JSON 객체로 처리하려면 json_lines 또는 split 필터 사용 고려
    # 또는 codec => json 으로 받고, filter에서 split 플러그인 사용
    # 압축 로그 처리를 위한 필수 설정
    decompress_request => true # Content-Encoding: gzip 헤더가 있는 경우 자동으로 압축 해제
    # API 키는 헤더에서 읽어 별도 처리하거나, Logstash 설정에서 직접 검증 가능
    # request_headers_target => "http_request_headers" # 헤더를 특정 필드에 저장
  }
}

filter {
  # CHO:LOG 로그는 이미 배열로 오므로, 개별 로그 이벤트로 분리해야 할 수 있음
  # 만약 input codec을 json으로 받았다면:
  # split {
  #   field => "message" # 실제 로그 배열 필드명 확인 필요 (보통 body 전체)
  # }

  # 수신된 JSON 로그에 대한 추가 처리
  date {
    match => [ "timestamp", "ISO8601" ] # CHO:LOG 로그의 timestamp 필드 형식
  }
  
  # serviceName, environment는 로그 본문에 이미 포함되어 있음
  #mutate {
  #  rename => { "[apiKey]" => "api_key_from_log" } # 로그 내 apiKey 필드 이름 변경 (저장 시 주의)
  #}

  # API 키 검증 예시 (Logstash 변수 또는 외부 파일 사용 권장)
  # if [http_request_headers][X-API-Key] != "your-expected-api-key" {
  #   drop { } # 유효하지 않은 API 키면 로그 삭제
  # }
}

output {
  elasticsearch {
    hosts => ["http://your-elasticsearch-host:9200"]
    index => "cholog-%{[serviceName]}-%{+YYYY.MM.dd}" # 로그 내 serviceName 필드 사용
    # user => "elastic"
    # password => "your_elastic_password"
  }
  # 디버깅을 위해 콘솔 출력 추가 (선택 사항)
  # stdout { codec => rubydebug }
}
```

> **중요**: 로그 압축 기능을 사용하는 경우(`cholog.logger.compress-logs: true`), 반드시 Logstash HTTP input 플러그인에 `decompress_request => true` 설정을 추가해야 합니다. `codec` 설정은 `json_lines` 또는 `json` 후 `split` 필터를 사용하여 로그 배열을 개별 이벤트로 처리하는 것을 고려해야 합니다.

**2. Logstash 실행:**

```bash
./bin/logstash -f logstash-cholog.conf
```

**3. Kibana에서 로그 확인:**

Kibana 대시보드에서 `cholog-*` 인덱스 패턴을 생성하여 수집된 로그를 검색, 시각화하고 분석할 수 있습니다. `serviceName`, `environment`, `requestId` (trace ID), `throwable.className` 등의 필드를 활용하여 강력한 분석이 가능합니다.

## 최적화된 로그 형식 (예시)

```json
{
  "timestamp": "2025-05-15T10:30:05.123Z",
  "level": "INFO",
  "logger": "com.example.service.OrderService",
  "message": "Order ORD12345 processed successfully for user usr_abc",
  "thread": "http-nio-8080-exec-5",
  "sequence": 1,
  "serviceName": "order-processing-service",
  "version": "1.2.5",
  "environment": "prod,aws-eu-central-1",
  "hostName": "appserver-prod-01.example.com",
  "ipAddress": "10.0.1.100",
  "serverPort": "8080",
  "apiKey": "[FILTERED]",
  "requestId": "frontend-generated-uuid-789xyz",
  "clientIp": "203.0.113.45",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
  "uaMobile": false,
  "uaPlatform": "Windows",
  "responseHeaders": {
    "Content-Type": "application/json;charset=UTF-8",
    "X-Correlation-ID": "backend-corr-id-567"
  },
  "headers": {
    "content-type": "application/json",
    "accept": "application/json",
    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
    "x-request-id": "frontend-generated-uuid-789xyz",
    "sec-ch-ua": "\"Chromium\";v=\"123\", \"Not(A:Brand\";v=\"24\"",
    "sec-ch-ua-mobile": "?0",
    "sec-ch-ua-platform": "\"Windows\""
  },
  "mdcContext": {
    "userId": "usr_abc",
    "tenantId": "tenant_123",
    "request_param_source": "mobile_app",
    "orderId": "ORD987"
  },
  "performanceMetrics": {
    "cpuUsage": 35,
    "memoryUsage": 768,
    "activeThreads": 25,
    "totalThreads": 150
  },
  "http": {
    "requestMethod": "POST",
    "requestUri": "/api/v1/orders",
    "httpStatus": 201,
    "responseTime": 125
  },
  "error": {
    "type": "java.lang.NullPointerException",
    "message": "Cannot invoke \"String.length()\" because \"str\" is null",
    "stacktrace": [
      "at com.example.service.OrderService.processOrder(OrderService.java:125)",
      "at com.example.controller.OrderController.createOrder(OrderController.java:57)"
    ]
  },
  "filtered": true
}
```
*(위 JSON 예시는 `apiKey`가 `sensitive-patterns`에 포함되어 필터링된 경우를 가정합니다.)*

## 고급 설정 및 커스터마이징

### 필터 순서 커스터마이징

CHO:LOG가 등록하는 서블릿 필터들(`RequestTimingFilter` 등)의 실행 순서를 변경하거나, 기존 필터들과의 순서를 조정해야 하는 경우 Spring Boot의 표준 방식을 사용하여 `FilterRegistrationBean`을 직접 정의할 수 있습니다.

### 사용자 정의 로그 필드 추가 (MDC 활용)

SLF4J의 MDC(Mapped Diagnostic Context)를 사용하여 현재 스레드 컨텍스트에 특정 정보를 추가하면, CHO:LOG는 해당 정보를 자동으로 수집하여 로그의 `mdcContext` 필드에 포함시킵니다.

```java
import org.slf4j.MDC;

// ...
try (MDC.MDCCloseable mdcCloseable = MDC.putCloseable("custom.orderId", "ORD12345")) {
    log.info("주문 처리 중...");
    // 이 블록 내에서 발생하는 모든 로그의 mdcContext에는 {"custom.orderId": "ORD12345"} 정보가 포함됩니다.
}
```

### 프론트엔드와 백엔드 간 요청 추적 (X-Request-Id)

CHO:LOG는 프론트엔드에서 전달된 `X-Request-Id` HTTP 헤더가 있는 경우 이를 감지하여 로그의 `requestId` 필드로 사용합니다. 이를 통해 프론트엔드와 백엔드 간, 또는 마이크로서비스 간 요청 흐름을 추적할 수 있습니다. 프론트엔드 애플리케이션에서 해당 헤더를 설정해주면 됩니다.

## 문제 해결 (Troubleshooting)

-   **로그가 전송되지 않는 경우**:
    -   `cholog.logger.url` 설정이 올바른지, 해당 URL로 네트워크 연결이 가능한지 확인합니다.
    -   Logstash 또는 중앙 로그 서버의 입력 플러그인이 정상적으로 실행 중이고 JSON 요청을 수신 대기하고 있는지 확인합니다.
    -   애플리케이션 로그에서 CHO:LOG SDK 관련 에러 메시지가 없는지 확인합니다 (특히 `LogSenderService` 관련).
    -   `cholog.logger.api-key` 및 `cholog.logger.service-name`이 올바르게 설정되었는지 확인합니다.

-   **민감 정보가 필터링되지 않는 경우**:
    -   `cholog.logger.sensitive-patterns` 설정이 올바른지, 필터링하려는 JSON 경로 패턴과 일치하는지 확인합니다.
    -   `RequestTimingFilter`의 경우, 파라미터 키 자체에 대한 민감 키워드 필터링이 우선 적용될 수 있습니다.

-   **디스크 큐 저장 오류가 발생하는 경우**:
    -   디스크 큐 디렉토리의 쓰기 권한이 있는지 확인합니다.
    -   디스크 큐 경로가 유효한지 확인합니다.
    -   최대 디스크 큐 크기 설정이 적절한지 확인합니다.

-   **압축된 로그가 ELK에서 제대로 처리되지 않는 경우**:
    -   Logstash HTTP Input 플러그인에 `decompress_request => true` 설정이 추가되어 있는지 확인합니다.
    -   CHO:LOG에서 `cholog.logger.compress-logs: true` (또는 유사한 설정명)이 활성화되어 `Content-Encoding: gzip` 헤더와 함께 전송되는지 확인합니다.

## 상태 지표 모니터링

CHO:LOG는 JMX를 통해 로그 전송 상태와 성능 지표를 모니터링할 수 있습니다. `cholog.logger.expose-metrics-via-jmx: true` (기본값) 설정 시 활성화됩니다.

JConsole이나 다른 JMX 클라이언트를 통해 다음 MBean에서 모니터링 지표를 확인할 수 있습니다:
`com.cholog.logger:type=LogSenderMetrics`

주요 지표:
-   `QueueSize`: 현재 메모리 큐에 있는 로그 수
-   `IsServerAvailable`: 중앙 로그 서버 연결 가능 여부
-   `DiskQueueFileCount`: 디스크 큐에 저장된 파일 수
-   `DiskQueueTotalSize`: 디스크 큐의 총 크기 (바이트)
-   `ProcessedLogsCount`: 성공적으로 전송 처리된 총 로그 수
-   `FailedLogsCount`: 전송 실패 또는 디스크에 저장된 로그 수

## 라이선스

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

이 SDK는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참고하세요.
