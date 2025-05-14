# cholog-server

로그 수집 및 조회를 위한 Spring Boot 기반 서버 애플리케이션입니다.

## 프로젝트 구조

```
.
├── gradle/wrapper/            # Gradle Wrapper
├── S12P31B207/                # (내용 미확인 - 필요시 추가 설명)
├── src
│   ├── main
│   │   ├── java/com/example/logserver
│   │   │   ├── LogServerApplication.java  # Spring Boot 애플리케이션 진입점
│   │   │   ├── config/
│   │   │   │   └── JacksonConfig.java     # Jackson JSON (역)직렬화 설정
│   │   │   ├── controller/
│   │   │   │   └── LogController.java     # 로그 관련 API 엔드포인트
│   │   │   ├── dto/
│   │   │   │   └── LogFilterRequest.java  # 로그 필터링 요청 DTO
│   │   │   ├── model/
│   │   │   │   └── LogEntry.java          # 로그 데이터 모델
│   │   │   ├── repository/
│   │   │   │   └── LogRepository.java     # 인메모리 로그 저장소
│   │   │   ├── service/
│   │   │   │   ├── FilterService.java     # 로그 필터 옵션 제공 서비스
│   │   │   │   ├── LogService.java        # 핵심 로그 처리 서비스
│   │   │   │   └── StatisticsService.java # 로그 통계 서비스
│   │   │   └── util/
│   │   │       ├── DateTimeUtil.java      # 날짜/시간 유틸리티
│   │   │       └── LogEntryDeserializer.java # LogEntry 커스텀 역직렬화
│   │   └── resources/
│   │       └── templates/
│   │           └── index.html           # 로그 대시보드 UI (Thymeleaf + JavaScript)
│   │       └── (application.properties 또는 application.yml 등 설정 파일 위치)
│   └── test
│       └── java/com/example/logserver
│           └── (테스트 코드 위치)
├── .gitignore                 # Git 무시 파일 목록
├── build.gradle               # Gradle 빌드 스크립트
├── gradlew                    # Gradle Wrapper (Linux/Mac)
├── gradlew.bat                # Gradle Wrapper (Windows)
├── README.md                  # 프로젝트 설명 (현재 파일)
└── settings.gradle            # Gradle 프로젝트 설정
```

## 주요 모듈 설명

### `LogServerApplication.java`
- Spring Boot 애플리케이션의 메인 클래스입니다.
- `main` 메소드를 통해 애플리케이션을 실행합니다.

### `config/JacksonConfig.java`
- Jackson 라이브러리의 설정을 담당합니다.
- `ObjectMapper` 빈을 생성하여 JSON 직렬화/역직렬화 방식을 정의합니다.
    - 기본 `ObjectMapper`: 직렬화 시 `null` 필드 제외, `LogEntryDeserializer` 사용
    - `objectMapperWithNulls`: 직렬화 시 `null` 필드 포함, `LogEntryDeserializer` 사용
    - `MappingJackson2HttpMessageConverter`: HTTP 응답 시 `null` 필드를 항상 포함하도록 설정, `LogEntryDeserializer` 사용

### `controller/LogController.java`
- 로그 관련 HTTP 요청을 처리하는 REST 컨트롤러입니다.
- 주요 API 엔드포인트:
    - `POST /logs`: 로그 데이터를 수신합니다.
    - `GET /logs`: 필터링된 로그 목록을 조회합니다.
    - `GET /filters`: 사용 가능한 필터 옵션 목록을 조회합니다.
    - `GET /logs/full`: 필터링된 로그의 전체 상세 정보를 조회합니다.
    - `GET /logs/detail/{index}`: 특정 인덱스의 로그 상세 정보를 조회합니다.
    - `GET /`: 웹 UI (`index.html`)를 위한 메인 페이지를 반환합니다.
    - `GET /stats`: 로그 통계 정보를 조회합니다.
    - `GET /stats/charts`: 차트 표시를 위한 통계 데이터를 조회합니다.
    - API 키 및 서비스 이름을 기반으로 로그를 조회하는 다양한 엔드포인트가 존재합니다.

### `dto/LogFilterRequest.java`
- 로그 필터링 요청 시 사용되는 DTO (Data Transfer Object)입니다.
- 필터링 조건으로 사용될 수 있는 다양한 필드(API 키, 경로, 레벨, 상태 코드, 서비스 이름, 검색어, 플랫폼, 모바일 여부, 시간 범위 등)를 포함합니다.
- 여러 이름으로 전달될 수 있는 파라미터(상태 코드, 서비스 이름)를 통합적으로 처리하는 메소드를 제공합니다.

### `model/LogEntry.java`
- 로그 한 건의 데이터를 나타내는 모델 클래스입니다.
- 로그 레벨, 메시지, 타임스탬프, 서비스 이름, 요청 정보, 사용자 에이전트, 성능 지표 등 다양한 로그 관련 정보를 필드로 가집니다.
- Lombok의 `@Data` 어노테이션을 사용하여 getter, setter 등을 자동으로 생성합니다.
- `optimizeFields()` 메소드를 통해 비어있는 필드를 정리합니다.

### `repository/LogRepository.java`
- 로그 데이터를 메모리에 저장하고 관리하는 저장소 클래스입니다.
- `LinkedList`를 사용하여 로그를 저장하며, 최대 저장 개수(`MAX_LOGS`)를 제한합니다 (최신 로그 우선).
- 로그 추가, 전체 조회, 다양한 조건에 따른 필터링된 로그 조회, 특정 인덱스의 로그 상세 정보 조회 기능을 제공합니다.
- 모든 메소드는 `synchronized` 키워드를 사용하여 스레드 안전성을 보장합니다.

### `service/LogService.java`
- 로그 처리의 핵심 비즈니스 로직을 담당하는 서비스 클래스입니다.
- `LogController`로부터 요청을 받아 로그 데이터를 수신, 파싱, 변환 후 `LogRepository`에 저장합니다.
- 로그 조회 및 필터링 로직을 수행합니다.
- `LogEntry` 객체를 `Map<String, Object>` 형태로 변환하여 API 응답에 사용합니다.

### `service/FilterService.java`
- 로그 필터링에 사용될 수 있는 동적인 옵션들을 제공하는 서비스입니다.
- 현재 저장된 로그 데이터를 기반으로 API 키, 로그 레벨, 서비스명, 경로, HTTP 상태 코드, 플랫폼 등의 고유한 값 목록을 추출하여 반환합니다.

### `service/StatisticsService.java`
- 로그 데이터를 기반으로 다양한 통계 정보를 생성하고 제공하는 서비스입니다.
- 주요 통계 항목:
    - 총 로그 건수
    - 로그 레벨별 분포
    - HTTP 상태 코드별 분포
    - 평균/최대/최소 응답 시간
    - 가장 많이 요청된 URI (TOP 10)
    - 주요 에러 메시지 분포 (TOP 10)
    - 플랫폼별 (OS, 브라우저 등) 분포
    - 모바일/데스크톱 분포
    - 시간대별 로그 발생 건수
- 차트 표시에 적합한 형태로 가공된 통계 데이터를 제공하는 기능도 포함합니다.

### `util/DateTimeUtil.java`
- 날짜 및 시간 관련 편의 기능을 제공하는 유틸리티 클래스입니다.
- 다양한 형식의 타임스탬프 문자열을 `LocalDateTime` 객체로 파싱합니다.
- 특정 타임스탬프가 기준 시간 이후인지 비교하는 기능을 제공합니다.

### `util/LogEntryDeserializer.java`
- Jackson 라이브러리에서 사용되는 커스텀 Deserializer입니다.
- JSON 데이터를 `LogEntry` 객체로 역직렬화하는 로직을 담당합니다.
- 표준 필드뿐만 아니라 중첩된 JSON 객체나 배열도 처리하여 `LogEntry`의 해당 필드에 매핑합니다.
- 역직렬화 후 `LogEntry.optimizeFields()`를 호출하여 불필요한 빈 필드를 정리합니다.

## 프론트엔드 대시보드 (`src/main/resources/templates/index.html`)

이 프로젝트는 웹 기반의 로그 대시보드 UI를 제공하며, `index.html` 파일을 통해 구현되어 있습니다. Thymeleaf를 사용하여 서버 사이드 렌더링을 일부 활용하며, 대부분의 동적 기능은 JavaScript로 구현됩니다.

### 주요 기능
- **실시간 로그 조회 및 필터링**:
    - 다양한 필터 옵션 제공: API 키, 서비스, 로그 레벨, HTTP 상태 코드, 경로(URL), 메시지 내 키워드 검색, 시간 범위 (최근 1시간, 6시간, 24시간, 7일, 30일).
    - 필터 적용 시 해당 조건에 맞는 로그 목록을 비동기적으로 로드하여 테이블에 표시합니다.
    - 로그 목록은 주기적으로 자동 업데이트됩니다.
- **로그 상세 정보**:
    - 테이블의 각 로그 항목을 클릭하면 모달창을 통해 상세 정보를 확인할 수 있습니다.
    - 상세 정보는 '형식화된 보기' 탭과 '원본 JSON' 탭으로 제공됩니다.
    - '형식화된 보기'는 기본 정보, 요청 정보, 성능 지표, MDC 컨텍스트, 헤더 정보, 오류 정보 등으로 구분되어 가독성 있게 표시됩니다.
- **통계 시각화**:
    - Chart.js 라이브러리를 사용하여 로그 통계 데이터를 시각화합니다.
    - 통계 모달을 통해 다음 정보를 차트로 확인할 수 있습니다:
        - 시간별 로그 발생 건수 (KST 기준)
        - 로그 레벨 분포
        - HTTP 상태 코드 분포
    - 통계 요약 정보 (총 로그 수, 에러 로그 수, 평균 응답 시간)도 함께 제공됩니다.
- **사용자 인터페이스**:
    - 깔끔하고 반응형인 UI를 제공합니다.
    - 필터 컨트롤, 로그 테이블, 모달창 등이 직관적으로 구성되어 있습니다.
    - KST(한국 표준시) 기준으로 타임스탬프를 변환하여 표시합니다.

## 빌드 및 실행

### 빌드
```bash
./gradlew build
```

### 실행
```bash
java -jar build/libs/cholog-server-0.0.1-SNAPSHOT.jar
```
(JAR 파일 이름은 `build.gradle` 설정에 따라 다를 수 있습니다.)

애플리케이션은 기본적으로 내장 Tomcat 서버를 사용하여 실행됩니다. (포트 등은 `application.properties` 또는 `application.yml`에서 설정 가능)

## API 사용 예시 (Postman 등 사용)

- **로그 전송 (단일 로그):**
  ```
  POST /logs
  Content-Type: application/json

  {
    "level": "INFO",
    "message": "User logged in successfully",
    "timestamp": "2024-07-30T10:00:00.123Z",
    "serviceName": "auth-service",
    "apiKey": "your-api-key",
    "requestUri": "/api/login",
    "httpStatus": 200
  }
  ```

- **로그 전송 (배치 로그):**
  ```
  POST /logs
  Content-Type: application/json

  [
    {
      "level": "INFO",
      "message": "User logged in successfully",
      "timestamp": "2024-07-30T10:00:00.123Z",
      "serviceName": "auth-service",
      "apiKey": "your-api-key",
      "requestUri": "/api/login",
      "httpStatus": 200
    },
    {
      "level": "WARN",
      "message": "High CPU usage detected",
      "timestamp": "2024-07-30T10:00:05.456Z",
      "serviceName": "monitoring-service",
      "apiKey": "your-api-key"
    }
  ]
  ```

- **필터링된 로그 조회:**
  `GET /logs?level=ERROR&serviceName=auth-service&apiKey=your-api-key`

- **통계 조회:**
  `GET /stats?timeRange=24h&apiKey=your-api-key`

- **필터 옵션 조회:**
  `GET /filters`

---