# API 명세

# CHO:LOG API

---

## CHO:LOG API 공통 설정

CHO:LOG API에서 공통으로 사용하는 요청 형식과 응답 형식을 설명합니다.

### 주의!

CHO:LOG이 SUPERAPP에 제공하는 API는 사용자의 **직접 호출이 아닌**, CHO:LOG이 제공하는 SDK를 설치하여 자동으로 호출되는 것을 의도하고 있습니다.

**SDK 설치 방법**

[https://www.cholog.com/guide](https://www.cholog.com/guide)
혹은

[https://cholog.ssafyapp.com/guide](https://www.cholog.com/guide)

---

---

### API URL

공통요청 API URL은 다음과 같습니다.

HTTP

---

[https://cholog-server.shop/api](https://dukd0c8.ssafyapp.com/api/v1) (로그 수신 서버)

---

## CHO:LOG API Reference

### 1. JAVA 로그 저장

JAVA 로그 저장 API는 Java SDK가 송신한 로그를 서버에 저장할 수 있는 API입니다.

### 요청 URL

| 메서드 | URL |
| --- | --- |
| POST | /be |

### Request Body

**입력형태**

```json
[
	{
	  "timestamp": "string (ISO-8601 형식, 예: '2025-05-21T15:30:45.123Z')",
	  "level": "string (예: 'INFO', 'ERROR', 'WARN', 'DEBUG')",
	  "logger": "string",
	  "message": "string",
	  "thread": "string",
	  "sequence": "number (long)",
	  "source": "string (기본값: 'backend')",
	  "logType": "string (예: 'general', 'error')",
	  
	  "serviceName": "string",
	  "environment": "string",
	  "version": "string",
	  "apiKey": "string",
	  "projectKey": "string",
	  
	  "hostName": "string",
	  "ipAddress": "string",
	  "serverPort": "string",
	  
	  "requestId": "string",
	  "clientIp": "string",
	  "userAgent": "string",
	  "uaMobile": "boolean",
	  "uaPlatform": "string",
	  
	  "http": {
	    "requestMethod": "string",
	    "requestUri": "string",
	    "httpStatus": "number (integer)",
	    "responseTime": "number (long)"
	  },
	  
	  "headers": {
	    "string": "string"
	  },
	  
	  "responseHeaders": {
	    "string": "string"
	  },
	  
	  "mdcContext": {
	    "string": "any"
	  },
	  
	  "error": {
	    "message": "string",
	    "type": "string",
	    "stacktrace": "string",
	    // 추가 필드는 Object 타입으로 저장될 수 있음
	  },
	  
	  "performanceMetrics": {
	    "memoryUsage": "number (long)",
	    "cpuUsage": "number (long)",
	    "activeThreads": "number (integer)",
	    "totalThreads": "number (integer)"
	  }
	}
]
```

**예시**

```json
[
	{
	  "timestamp": "2025-05-21T15:30:45.123Z",
	  "level": "INFO",
	  "logger": "com.example.LogService",
	  "message": "사용자 로그인 시도",
	  "thread": "http-nio-8080-exec-1",
	  "sequence": 12345,
	  "source": "backend",
	  "logType": "general",
	  
	  "serviceName": "user-service",
	  "environment": "production",
	  "version": "1.2.3",
	  "apiKey": "api-key-123456",
	  "projectKey": "project-123",
	  
	  "hostName": "app-server-01",
	  "ipAddress": "10.0.0.1",
	  "serverPort": "8080",
	  
	  "requestId": "req-uuid-123456",
	  "clientIp": "192.168.1.100",
	  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
	  "uaMobile": false,
	  "uaPlatform": "Windows",
	  
	  "http": {
	    "requestMethod": "POST",
	    "requestUri": "/api/users/login",
	    "httpStatus": 200,
	    "responseTime": 150
	  },
	  
	  "headers": {
	    "Content-Type": "application/json",
	    "Accept": "application/json"
	  },
	  
	  "responseHeaders": {
	    "Content-Type": "application/json",
	    "Content-Length": "256"
	  },
	  
	  "mdcContext": {
	    "userId": "user-123",
	    "sessionId": "session-456"
	  },
	  
	  "error": {
	    "message": "인증 실패",
	    "type": "AuthenticationException",
	    "stacktrace": "com.example.AuthenticationException: 인증 실패\n\tat com.example.SecurityService.authenticate(SecurityService.java:45)"
	  },
	  
	  "performanceMetrics": {
	    "memoryUsage": 256,
	    "cpuUsage": 15,
	    "activeThreads": 10,
	    "totalThreads": 20
	  }
	}
]
```

### 성공응답

성공응답이 없습니다.

### 실패응답

실패응답이 없습니다.

### 2. Javascript 로그 저장

Javascript 로그 저장 API는 Javascript SDK가 송신한 로그를 저장할 수 있는 API입니다.

### 요청 URL

| 메서드 | URL |
| --- | --- |
| POST | /js |

### Request Body

**입력형태**

```json
[
	{
	  "timestamp": "string (ISO-8601 형식, 예: '2025-05-21T15:30:45.123Z')",
	  "sequence": "number (long)",
	  "level": "string (예: 'INFO', 'ERROR', 'DEBUG', 'TRACE', 'WARN')",
	  "message": "string",
	  "source": "string (예: 'frontend')",
	  "projectKey": "string",
	  "environment": "string",
	  "requestId": "string",
	  "logger": "string (예: 'cholog', 'console')",
	  "logType": "string (예: 'general', 'error', 'network', 'event')",
	  
	  "payload": {
	    "string": "any"
	  },
	  
	  "error": {
	    "type": "string",
	    "message": "string",
	    "stacktrace": "string"
	  },
	  
	  "http": {
	    "method": "string",
	    "requestUri": "string",
	    "status": "number (integer)",
	    "responseTime": "number (long)"
	  },
	  
	  "client": {
	    "url": "string",
	    "userAgent": "string",
	    "referrer": "string"
	  },
	  
	  "event": {
	    "type": "string",
	    "targetSelector": "string",
	    "properties": {
	      "string": "any"
	    }
	  }
	}
]
```

**예시**

```json
[
	{
	  "timestamp": "2025-05-21T11:56:51.024Z", // 밀리초 세 자리
	  "sequence": "로그 시퀀스 번호",
	  "level": "로그 레벨 (INFO, ERROR, DEBUG, TRACE, WARN 등)",
	  "message": "로그 메시지",
	  "source": "frontend",
	  "projectKey": "프로젝트 식별 키",
	  "environment": "실행 환경 (dev, local, prod 등)",
	  "requestId": "요청 추적 ID",
	  "logger": "로그를 생성한 로거의 이름 (cholog, console 등)",
	  "logType": "로그 유형 (general, error, network, event 등)",
	  
	  "payload": {
	    "임의 키": "임의 값",
	    "추가 데이터": "값"
	  },
	  
	  "error": {
	    "type": "오류 타입",
	    "message": "오류 메시지",
	    "stacktrace": "스택 트레이스"
	  },
	  
	  "http": {
	    "method": "HTTP 메서드",
	    "requestUri": "요청 URI",
	    "status": "HTTP 상태 코드",
	    "responseTime": "응답 시간(ms)"
	  },
	  
	  "client": {
	    "url": "현재 페이지 URL",
	    "userAgent": "사용자 에이전트 문자열",
	    "referrer": "참조 URL (이전 페이지)"
	  },
	  
	  "event": {
	    "type": "이벤트 타입 (click, submit 등)",
	    "targetSelector": "이벤트 대상 요소의 CSS 선택자",
	    "properties": {
	      "이벤트 속성": "값",
	      "추가 이벤트 정보": "값"
	    }
	  }
	}
]
```

### 성공응답

성공응답이 없습니다.

### 실패응답

실패응답이 없습니다.
