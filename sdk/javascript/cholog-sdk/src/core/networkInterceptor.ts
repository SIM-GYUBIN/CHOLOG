// src/core/networkInterceptor.ts
import { TraceContext } from "./traceContext";
import { Logger } from "./logger";

export class NetworkInterceptor {
  private static isInitialized = false;
  // 원래 함수들을 저장할 변수
  private static originalFetch: typeof window.fetch | null = null;
  private static originalXhrSend: typeof XMLHttpRequest.prototype.send | null =
    null;
  // 필요시 open도 저장: private static originalXhrOpen: typeof XMLHttpRequest.prototype.open | null = null;

  // private static generateRequestId(): string {
  //   if (crypto && crypto.randomUUID) {
  //     return crypto.randomUUID();
  //   } else {
  //     // 매우 기본적인 fallback (UUID v4만큼 강력하지 않음)
  //     console.warn(
  //       "crypto.randomUUID is not available. Using basic fallback for Request ID."
  //     );
  //     return `fallback-${Date.now()}-${Math.random()
  //       .toString(36)
  //       .substring(2, 15)}`;
  //   }
  // }

  /**
   * window.fetch를 패치하여 X-Request-ID 헤더를 추가
   */
  private static patchFetch(): void {
    // 원본 fetch 저장
    this.originalFetch = window.fetch;
    // NetworkInterceptor의 this 컨텍스트를 유지하기 위해 self 사용
    const self = this;

    // 새로운 fetch 함수 정의
    window.fetch = async (
      input: RequestInfo | URL,
      init?: RequestInit
    ): Promise<Response> => {
      const requestUrl =
        typeof input === "string" ? input : (input as URL).toString();
      const sdkLogApiEndpoint = Logger.getApiEndpoint(); // Logger 내부의 고정된 값

      if (sdkLogApiEndpoint && requestUrl.startsWith(sdkLogApiEndpoint)) {
        return NetworkInterceptor.originalFetch!.call(window, input, init);
      }

      let traceId = TraceContext.getCurrentTraceId();
      let isNewTrace = false;
      if (!traceId) {
        traceId = TraceContext.startNewTrace(); // 새 트레이스 시작
        isNewTrace = true;
        // Logger.info("New trace started for network request", { traceId }); // 필요시 로그
      }

      // init 객체가 없을 수도 있으므로 안전하게 복사 또는 생성
      // 원본 init 객체를 수정하지 않기 위해 얕은 복사 사용
      const modifiedInit = { ...(init || {}) };

      // headers 객체 관리 (다양한 타입 처리)
      let currentHeaders = modifiedInit.headers;
      let newHeaders: Headers;

      if (currentHeaders instanceof Headers) {
        // Headers 객체면 복제해서 사용 (불변성 유지)
        newHeaders = new Headers(currentHeaders);
      } else if (Array.isArray(currentHeaders)) {
        // 배열이면 Headers 객체로 변환
        newHeaders = new Headers(currentHeaders);
      } else if (
        typeof currentHeaders === "object" &&
        currentHeaders !== null
      ) {
        // 일반 객체면 Headers 객체로 변환
        newHeaders = new Headers(currentHeaders as Record<string, string>);
      } else {
        // 그 외의 경우 (undefined 등) 빈 Headers 객체 생성
        newHeaders = new Headers();
      }

      // 새로운 Headers 객체에 Request ID 추가
      newHeaders.set("X-Request-ID", traceId);
      modifiedInit.headers = newHeaders;

      const startTime = Date.now();
      Logger.info(
        `API Request START: ${
          typeof input === "string" ? input : (input as URL).toString()
        }`,
        { traceId, method: modifiedInit.method || "GET" }
      );

      try {
        const response = await self.originalFetch!.call(
          window,
          input,
          modifiedInit
        );
        const duration = Date.now() - startTime;
        Logger.info(`API Request END: ${response.status} ${response.url}`, {
          traceId,
          status: response.status,
          durationMs: duration,
        });
        return response;
      } catch (error) {
        const duration = Date.now() - startTime;
        Logger.error(
          `API Request FAILED: ${
            typeof input === "string" ? input : (input as URL).toString()
          }`,
          { traceId, error, durationMs: duration }
        );
        throw error;
      } finally {
        // 만약 이 fetch가 독립적인 트레이스의 시작이었다면, 여기서 트레이스를 종료할지 고민.
        // 보통은 사용자 액션이 끝날 때까지 트레이스가 유지됨.
        // if (isNewTrace) TraceContext.setCurrentTraceId(null); // 상황에 따라
      }
    };
  }

  /**
   * XMLHttpRequest.prototype.send를 패치하여 X-Request-ID 헤더를 추가
   */
  private static patchXMLHttpRequest(): void {
    // 원본 send 저장
    this.originalXhrSend = XMLHttpRequest.prototype.send;
    // 원본 open 저장 (메소드와 URL 로깅에 필요)
    const originalXhrOpen = XMLHttpRequest.prototype.open;

    // XMLHttpRequest.prototype.open 패치 (메소드와 URL 정보를 저장하기 위해)
    XMLHttpRequest.prototype.open = function (
      method: string,
      url: string | URL,
      async?: boolean,
      username?: string | null,
      password?: string | null
    ): void {
      // XHR 인스턴스에 메소드와 URL 정보를 저장 (커스텀 속성 사용)
      // Symbol을 사용하면 충돌 가능성을 줄일 수 있습니다.
      // 예: (this as any)._chologMethod = method;
      // 간단하게는 그냥 속성으로 저장
      (this as any)._chologMethod = method;
      (this as any)._chologUrl = typeof url === "string" ? url : url.toString();

      // 원본 open 호출
      originalXhrOpen.apply(this, arguments as any);
    };

    // 새로운 send 함수 정의 (function 키워드 사용 필수: this가 XHR 인스턴스를 가리키도록)
    XMLHttpRequest.prototype.send = function (
      body?: Document | XMLHttpRequestBodyInit | null
    ) {
      // send가 호출될 때의 this는 XHR 인스턴스
      const xhr = this as XMLHttpRequest & {
        _chologMethod?: string;
        _chologUrl?: string;
        _chologStartTime?: number;
        _chologTraceId?: string;
      };

      const requestUrl = xhr._chologUrl;
      const sdkLogApiEndpoint = Logger.getApiEndpoint(); // Logger 내부의 고정된 값

      if (
        sdkLogApiEndpoint &&
        requestUrl &&
        requestUrl.startsWith(sdkLogApiEndpoint)
      ) {
        return NetworkInterceptor.originalXhrSend!.apply(
          this,
          arguments as any
        );
      }

      // 1. Trace ID 가져오기 또는 생성
      let traceId = TraceContext.getCurrentTraceId();
      let isNewTrace = false;
      if (!traceId) {
        traceId = TraceContext.startNewTrace(); // 새 트레이스 시작
        isNewTrace = true;
      }
      // 생성/가져온 traceId를 XHR 인스턴스에도 저장 (리스너에서 사용)
      xhr._chologTraceId = traceId;

      // 2. X-Request-ID 헤더 설정
      // send가 호출되기 전에 setRequestHeader 호출 (open이 먼저 호출되어 있어야 함)
      try {
        // 이미 설정된 헤더를 덮어쓰지 않도록 주의할 수 있지만, 보통은 덮어쓰는 것이 맞습니다.
        this.setRequestHeader("X-Request-ID", traceId);
      } catch (e) {
        // open() 전에 send()가 호출되거나 다른 이유로 실패 시
        Logger.error("Cholog SDK: Failed to set X-Request-ID header for XHR.", {
          traceId,
          error: e,
          url: xhr._chologUrl,
        });
        // console.error도 유지하거나 Logger로 통일
      }

      // 3. 로깅을 위한 이벤트 리스너 추가 (원본 send 호출 전에!)
      const logStart = () => {
        xhr._chologStartTime = Date.now(); // 시작 시간 기록
        Logger.info(
          `API Request START: ${xhr._chologMethod || "UnknownMethod"} ${
            xhr._chologUrl || "UnknownURL"
          }`,
          {
            traceId: xhr._chologTraceId,
            method: xhr._chologMethod,
            type: "XHR",
          }
        );
        // 리스너 한번만 실행되도록 제거
        xhr.removeEventListener("loadstart", logStart);
      };

      const logEnd = () => {
        const duration = xhr._chologStartTime
          ? Date.now() - xhr._chologStartTime
          : undefined;
        // readyState === 4 (DONE) 이고 status 코드로 성공/실패 판단 가능
        if (xhr.readyState === 4) {
          if (xhr.status >= 200 && xhr.status < 400) {
            // 성공으로 간주 (3xx 리다이렉션도 포함될 수 있음)
            Logger.info(
              `API Request END: ${xhr.status} ${
                xhr.responseURL || xhr._chologUrl || "UnknownURL"
              }`,
              {
                traceId: xhr._chologTraceId,
                status: xhr.status,
                durationMs: duration,
                type: "XHR",
              }
            );
          } else if (xhr.status >= 400) {
            // 클라이언트 또는 서버 오류
            Logger.error(
              `API Request FAILED: ${xhr.status} ${
                xhr.responseURL || xhr._chologUrl || "UnknownURL"
              }`,
              {
                traceId: xhr._chologTraceId,
                status: xhr.status,
                statusText: xhr.statusText,
                durationMs: duration,
                type: "XHR",
              }
            );
          }
        }
        // 이벤트 리스너 정리
        xhr.removeEventListener("load", logEnd);
        xhr.removeEventListener("error", logEnd);
        xhr.removeEventListener("abort", logEnd);
        xhr.removeEventListener("timeout", logEnd); // timeout 이벤트도 고려
      };

      // loadstart: 요청이 시작될 때 (헤더 전송 직후, 응답 받기 전)
      xhr.addEventListener("loadstart", logStart);
      // load: 요청 성공 완료 시 (status 코드 확인 필요)
      xhr.addEventListener("load", logEnd);
      // error: 네트워크 오류 등 요청 실패 시
      xhr.addEventListener("error", logEnd);
      // abort: 요청 중단 시
      xhr.addEventListener("abort", logEnd);
      // timeout: 타임아웃 발생 시
      xhr.addEventListener("timeout", logEnd);

      // 4. 원본 send 호출
      // this.originalXhrSend가 static 이므로, self 사용 불필요. NetworkInterceptor.originalXhrSend 사용
      if (!NetworkInterceptor.originalXhrSend) {
        console.error("Original XHR send function not found!");
        Logger.error("Original XHR send function not found!", { traceId }); // 로그 추가
        // 에러 처리
        return;
      }
      // arguments를 사용하여 원래 send에 모든 인자 전달
      return NetworkInterceptor.originalXhrSend.apply(this, arguments as any);

      // finally 블록은 XHR 이벤트 모델에서는 직접적으로 사용하기 어려움
    };
  }

  /**
   * Network Interceptor를 초기화
   * fetch와 XMLHttpRequest에 대한 패치를 적용
   */
  public static init(): void {
    if (this.isInitialized) {
      console.warn("NetworkInterceptor is already initialized.");
      return;
    }
    // 전역 객체가 존재하는지 확인 (브라우저 환경인지 체크)
    if (
      typeof window === "undefined" ||
      typeof XMLHttpRequest === "undefined"
    ) {
      console.warn(
        "NetworkInterceptor: Not running in a browser environment? Skipping patch."
      );
      return;
    }

    try {
      this.patchFetch();
      this.patchXMLHttpRequest();
      this.isInitialized = true;
      console.log("Cholog NetworkInterceptor initialized successfully."); // 성공 로그 (선택 사항)
    } catch (error) {
      console.error(
        "Cholog SDK: Failed to initialize NetworkInterceptor.",
        error
      );
      // 초기화 실패 시 패치 복원 시도
      // this.restore();
    }
  }

  /**
   * 패치된 함수들을 원래대로 복원
   */
  // public static restore(): void {
  //   if (!this.isInitialized) return;

  //   if (this.originalFetch) {
  //     window.fetch = this.originalFetch;
  //   }
  //   if (this.originalXhrSend) {
  //     XMLHttpRequest.prototype.send = this.originalXhrSend;
  //   }
  //   // if (this.originalXhrOpen) { XMLHttpRequest.prototype.open = this.originalXhrOpen; } // open도 복원

  //   this.originalFetch = null;
  //   this.originalXhrSend = null;
  //   // this.originalXhrOpen = null;
  //   this.isInitialized = false;
  //   console.log("Cholog NetworkInterceptor restored original functions."); // 복원 로그 (선택 사항)
  // }
}
