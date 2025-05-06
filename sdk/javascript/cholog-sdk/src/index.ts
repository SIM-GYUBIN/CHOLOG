// src/index.ts
import { Logger } from "./core/logger";
import { NetworkInterceptor } from "./core/networkInterceptor";
import { ErrorCatcher } from "./core/errorCatcher";
import { EventTracker } from "./core/eventTracker";
import { TraceContext } from "./core/traceContext";
import { LogPayload } from "./types"; // LogPayload 타입 임포트

export const Cholog = {
  init: (config: { projectKey: string; environment: string /* 다른 옵션들 */ }) => {
    // 중요: Logger.init이 TraceContext 사용보다 먼저 호출될 경우,
    // Logger 내부에서 TraceContext 접근 시 ID가 없을 수 있음.
    // 여기서는 TraceContext를 먼저 초기화하거나, Logger.init 내부에서 TraceID 필요시 생성하도록 해야함.
    // 현재 Logger는 TraceContext.getCurrentTraceId()를 호출하므로, Trace가 먼저 시작되는 것이 좋음.
    TraceContext.startNewTrace(); // SDK 초기화 자체도 하나의 Trace로 볼 수 있음

    Logger.init({
      projectKey: config.projectKey,
      environment: config.environment,
    });
    NetworkInterceptor.init();
    ErrorCatcher.init();
    EventTracker.init(/* eventTracker options */); // 필요시 EventTracker 옵션 전달

    // SDK 초기화 성공 로그 (Logger.info는 이제 projectKey, environment를 알고 있음)
    Logger.info("Cholog SDK Initialized", {
      sdk: "cholog-js", // 예시 페이로드
      version: "0.1.0-dev", // SDK 버전 (하드코딩 또는 빌드 시 주입)
    });
  },

  // Logger의 자체 로깅 메서드들을 직접 노출
  // payload는 선택적 인자
  log: (message: string, payload?: LogPayload) => Logger.log(message, payload),
  info: (message: string, payload?: LogPayload) => Logger.info(message, payload),
  warn: (message: string, payload?: LogPayload) => Logger.warn(message, payload),
  error: (message: string, payload?: LogPayload) => {
    // Cholog.error("단순 메시지")는 직접 에러 객체를 생성하지 않으므로,
    // error 필드를 채우려면 Error 객체를 넘겨야 함.
    // 일반적인 사용은 ErrorCatcher가 자동으로 하도록 하고,
    // 사용자가 직접 Cholog.error를 쓸 때는 상세 에러 정보를 payload로 넘기도록 유도하거나,
    // Error 객체를 인자로 받도록 시그니처 변경 고려.
    // 여기서는 우선 단순 메시지와 페이로드만 받는 것으로 유지.
    // ErrorCatcher는 Logger.logError를 사용하므로 이 메서드와는 다름.
    Logger.error(message, payload);
  },
  debug: (message: string, payload?: LogPayload) => Logger.debug(message, payload),
  trace: (message: string, payload?: LogPayload) => Logger.trace(message, payload),

  // 필요하다면 TraceContext의 메서드도 일부 노출 가능
  // startNewTrace: () => TraceContext.startNewTrace(),
  // getCurrentTraceId: () => TraceContext.getCurrentTraceId(),
};

export default Cholog;
