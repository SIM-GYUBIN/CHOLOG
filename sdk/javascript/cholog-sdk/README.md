# Cholog SDK / 초록

안녕하세요! Cholog SDK를 사용해주셔서 감사합니다. Cholog는 여러분의 웹 애플리케이션에서 발생하는 로그와 네트워크 요청을 손쉽게 수집하고, 개발자가 운영하는 중앙 로그 서버로 전송하여 통합적인 분석 및 디버깅을 가능하게 하는 로깅 라이브러리입니다.

Cholog 사이트 : [https://cholog.com](https://cholog.com)

Backend(Springboot)에도 SDK를 추가하여 더 강력한 로깅을 활용해보세요!

현재 Cholog SDK는 다음과 같은 주요 기능을 제공합니다:

- 자동 콘솔 로그 수집: console.log, console.info, console.warn, console.error, console.debug, console.trace를 통해 출력되는 모든 로그를 자동으로 감지하고 수집합니다.
- 네트워크 요청 자동 로깅: Workspace API와 XMLHttpRequest를 통해 발생하는 모든 네트워크 요청과 응답(성공/실패, 상태 코드, 소요 시간 등)을 자동으로 로깅합니다.
- 분산 추적 지원 (Trace ID): 각 네트워크 요청 헤더에 X-Request-ID (Trace ID)를 자동으로 주입하고, 관련 로그에 동일한 Trace ID를 기록하여 요청의 흐름과 관련 로그를 쉽게 추적할 수 있도록 합니다.
- 간편한 커스텀 로그 API: Cholog.info(), Cholog.warn(), Cholog.error() 등 직관적인 API를 통해 원하는 시점에 커스텀 로그를 손쉽게 전송할 수 있습니다.
- 자동 에러 감지 (ErrorCatcher): 전역 에러(window.onerror, unhandledrejection)를 감지하여 자동으로 로그를 전송합니다. (구현에 따라)
  사용자 이벤트 추적 (EventTracker): 특정 DOM 요소의 클릭과 같은 사용자 상호작용 이벤트를 추적할 수 있습니다. (구현에 따라)

- 효율적인 로그 전송: 수집된 로그는 즉시 전송되지 않고, 내부 큐에 쌓여 정해진 간격(기본 1초) 또는 큐 크기(기본 100KB)에 따라 일괄(Batch) 전송되어 네트워크 부하를 최소화합니다.

---

## 설치 및 사용 방법

### 설치

```
npm install cholog-sdk
```

### SDK 초기화

애플리케이션의 가장 최상단(진입점, 예: main.ts 또는 App.tsx의 최상단)에서 Cholog.init() 메소드를 호출하여 SDK를 초기화합니다. 초기화는 한 번만 수행해야 합니다.

```javascript
// 애플리케이션의 진입 파일 (예: src/main.ts, src/index.js 등)
import Cholog from "cholog-sdk";

Cholog.init({
  projectKey: "YOUR_APPLICATION_KEY", // Cholog 서버에서 발급받은 고유 앱 키를 입력하세요. (필수)
  environment: "development", // 환경을 구분 할 수 있도록 지정해 주세요 ! env를 통해 동적으로 관리하는 것을 권장합니다.
});
```

### 수동 로그 전송 (커스텀 로그)

```javascript
// 수동 로그 전송 예시
Cholog.info("사용자 프로필 페이지 진입", { page: "profile", userId: "user_abc" });

try {
  // 중요한 작업 수행
  const result = await doSomethingCritical();
  Cholog.info("중요 작업 성공", { result });
} catch (e) {
  Cholog.error("중요 작업 실패", { error: e, context: "결제 모듈" });
}
```

---

# 문제 해결 및 문의

Cholog SDK 사용 중 문제가 발생하거나 궁금한 점이 있으시면 skb0606@gmail.com으로 문의 주세요.

팀SSAFY B207
