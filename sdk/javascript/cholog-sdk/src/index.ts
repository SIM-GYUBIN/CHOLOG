// src/index.ts
import { Logger } from "./core/logger";
import { NetworkInterceptor } from "./core/networkInterceptor";
import { ErrorCatcher } from "./core/errorCatcher";
import { EventTracker } from "./core/eventTracker";
import { RequestContext } from "./core/requestContext";
import { LogPayload, ChologConfig } from "./types"; // LogPayload 타입 임포트

export const Cholog = {
  init: (config: ChologConfig) => {
    RequestContext.startNewRequest(); // SDK 초기화 자체도 하나의 Trace로 볼 수 있음

    // config에서 옵션 값 가져오기 (기본값 true 설정)
    const {
      apiKey,
      environment,
      enableEventTracker = true, // 기본값 true
      enableErrorCatcher = true, // 기본값 true
      enableNetworkInterceptor = true, // 기본값 true
      loggerOptions,
    } = config;

    // Logger 초기화 (항상 필요)
    Logger.init({
      apiKey,
      environment,
      ...(loggerOptions || {}), // loggerOptions가 있으면 해당 값 사용
    });

    // 조건부 초기화
    if (enableNetworkInterceptor) {
      NetworkInterceptor.init();
    }
    if (enableErrorCatcher) {
      ErrorCatcher.init();
    }
    if (enableEventTracker) {
      EventTracker.init(); // eventTrackerOptions 전달
    }

    const loggedEnvironment = environment || "default (by Logger)"; // 사용자가 environment를 제공하면 그 값, 아니면 Logger가 기본값을 사용할 것임을 명시

    // SDK 초기화 성공 로그
    Logger.info("Cholog SDK Initialized", {
      sdk: "cholog-sdk",
      version: "1.0.7", // SDK 버전에 맞게 수정하세요.
      features: {
        eventTracker: enableEventTracker,
        errorCatcher: enableErrorCatcher,
        networkInterceptor: enableNetworkInterceptor,
      },
      configApplied: {
        projectKeyObfuscated: apiKey ? `${apiKey.substring(0, 3)}...` : "NOT_SET", // 실제 키 일부만 표시
        environment: loggedEnvironment,
        batchInterval: loggerOptions?.batchInterval || "default (Logger)",
        maxQueueSize: loggerOptions?.maxQueueSize || "default (Logger)",
      },
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
