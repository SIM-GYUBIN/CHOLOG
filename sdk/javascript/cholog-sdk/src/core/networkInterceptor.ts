// src/core/networkInterceptor.ts

export class NetworkInterceptor {
  private static isInitialized = false;
  // 원래 함수들을 저장할 변수
  private static originalFetch: typeof window.fetch | null = null;
  private static originalXhrSend: typeof XMLHttpRequest.prototype.send | null =
    null;
  // 필요시 open도 저장: private static originalXhrOpen: typeof XMLHttpRequest.prototype.open | null = null;

  private static generateRequestId(): string {
    if (crypto && crypto.randomUUID) {
      return crypto.randomUUID();
    } else {
      // 매우 기본적인 fallback (UUID v4만큼 강력하지 않음)
      console.warn(
        "crypto.randomUUID is not available. Using basic fallback for Request ID."
      );
      return `fallback-${Date.now()}-${Math.random()
        .toString(36)
        .substring(2, 15)}`;
    }
  }

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
      const requestId = self.generateRequestId();

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
      newHeaders.set("X-Request-ID", requestId);
      modifiedInit.headers = newHeaders;

      // 원본 fetch 호출 (this 컨텍스트 주의 - self.originalFetch 사용)
      // this.originalFetch가 null 아님을 단언 (!) 또는 null 체크
      if (!self.originalFetch) {
        console.error("Original fetch function not found!");
        // 에러를 던지거나 기본 fetch 동작을 시도할 수 있지만, 여기선 에러 로그만 남김
        // 이 경우는 init 로직에 문제가 있다는 의미일 수 있음
        return Promise.reject(new Error("Original fetch not available"));
      }
      // return self.originalFetch(input, modifiedInit);
      return self.originalFetch.call(window, input, modifiedInit);
    };
  }

  /**
   * XMLHttpRequest.prototype.send를 패치하여 X-Request-ID 헤더를 추가
   */
  private static patchXMLHttpRequest(): void {
    // 원본 send 저장
    this.originalXhrSend = XMLHttpRequest.prototype.send;
    // NetworkInterceptor의 this 컨텍스트 유지
    const self = this;

    // XMLHttpRequest.prototype.open도 필요시 여기서 함께 패치

    // 새로운 send 함수 정의 (function 키워드 사용 필수: this가 XHR 인스턴스를 가리키도록)
    XMLHttpRequest.prototype.send = function (
      body?: Document | XMLHttpRequestBodyInit | null
    ) {
      const requestId = self.generateRequestId();

      // send가 호출되기 전에 setRequestHeader 호출
      // (주의: open이 먼저 호출되어 있어야 함)
      try {
        this.setRequestHeader("X-Request-ID", requestId);
      } catch (e) {
        console.error(
          "Cholog SDK: Failed to set X-Request-ID header. Was XHR opened first?",
          e
        );
      }

      // 원본 send 호출 (this 컨텍스트는 XHR 인스턴스, arguments 전달)
      if (!self.originalXhrSend) {
        console.error("Original XHR send function not found!");
        // 에러 처리
        return;
      }
      // arguments를 사용하여 원래 send에 모든 인자 전달
      return self.originalXhrSend.apply(this, arguments as any);
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
