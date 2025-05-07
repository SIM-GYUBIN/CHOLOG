// src/index.ts
import { Logger } from "./core/logger";
import { NetworkInterceptor } from "./core/networkInterceptor";
import { ErrorCatcher } from "./core/errorCatcher";
import { EventTracker } from "./core/eventTracker";
import { TraceContext } from "./core/traceContext";
import { LogPayload } from "./types"; // LogPayload 타입 임포트

export const Cholog = {
  init: (config: { projectKey: string; environment: string /* 다른 옵션들 */ }) => {
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
    Logger.error(message, payload);
  },
  debug: (message: string, payload?: LogPayload) => Logger.debug(message, payload),
  trace: (message: string, payload?: LogPayload) => Logger.trace(message, payload),

  // 필요하다면 TraceContext의 메서드도 일부 노출 가능
  // startNewTrace: () => TraceContext.startNewTrace(),
  // getCurrentTraceId: () => TraceContext.getCurrentTraceId(),
};

export default Cholog;
