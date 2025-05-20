// src/core/networkInterceptor.ts
import { RequestContext } from "./requestContext";
import { Logger } from "./logger";
import { LogHttp, LogError, LogClient } from "../types";

export class NetworkInterceptor {
  private static isInitialized = false;
  private static originalFetch: typeof window.fetch | null = null;
  // XMLHttpRequest 관련 타입은 any로 처리하거나, 더 상세한 타입 정의 필요 시 추가
  private static originalXhrOpen: any = null;
  private static originalXhrSend: any = null;

  private static patchFetch(): void {
    if (typeof window === "undefined" || !window.fetch) return;
    this.originalFetch = window.fetch;

    window.fetch = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
      const requestUrl = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;

      // Cholog 자체 로그 전송 요청은 인터셉트하지 않음
      if (requestUrl.startsWith(Logger.getApiEndpoint())) {
        return NetworkInterceptor.originalFetch!.call(window, input, init);
      }

      const requestId = RequestContext.getCurrentRequestId() || RequestContext.startNewRequest();
      const modifiedInit = { ...(init || {}) };
      modifiedInit.headers = new Headers(modifiedInit.headers); // Ensure Headers object
      if (!modifiedInit.headers.has("X-Request-ID")) {
        // 이미 있다면 존중
        modifiedInit.headers.set("X-Request-ID", requestId);
      }

      const startTime = Date.now();
      const requestDetails: LogHttp = {
        requestMethod: (
          modifiedInit.method ||
          (typeof input !== "string" && !(input instanceof URL) ? input.method : "GET") ||
          "GET"
        ).toUpperCase(),
        requestUri: requestUrl,
      };

      // 요청 시작 로그 (선택적, 너무 많은 로그를 유발할 수 있음)
      // Logger.logHttp(`API Request START: ${requestDetails.method} ${requestDetails.requestUri}`, { request: requestDetails });

      try {
        const response = await NetworkInterceptor.originalFetch!.call(window, input, modifiedInit);
        const responseTime = Date.now() - startTime;

        Logger.logHttp(
          `Fetch 요청 => ${requestDetails.requestMethod} ${requestDetails.requestUri} - 상태 => ${response.status}`,
          {
            ...requestDetails,
            httpStatus: response.status,
            responseTime,
          },
          undefined // clientDetails
        );
        return response;
      } catch (error: any) {
        const responseTime = Date.now() - startTime;
        const errorDetails: LogError = {
          type: error?.name || "FetchError",
          message: error?.message || "Network request failed",
          stacktrace: error?.stack,
        };
        Logger.logHttp(
          `Fetch 오류 => ${requestDetails.requestMethod} ${requestDetails.requestUri}`,
          {
            ...requestDetails,
            responseTime,
          },
          undefined, // clientDetails
          errorDetails
        );
        throw error;
      }
    };
  }

  private static patchXMLHttpRequest(): void {
    if (typeof window === "undefined" || !window.XMLHttpRequest) return;

    this.originalXhrOpen = XMLHttpRequest.prototype.open;
    this.originalXhrSend = XMLHttpRequest.prototype.send;
    const self = this;

    XMLHttpRequest.prototype.open = function (method: string, url: string | URL /* ...other args */) {
      // Cholog 관련 속성 저장
      (this as any)._chologMethod = method;
      (this as any)._chologUrl = typeof url === "string" ? url : url.toString();

      // Cholog 자체 로그 전송 요청은 인터셉트하지 않음
      if ((this as any)._chologUrl.startsWith(Logger.getApiEndpoint())) {
        (this as any)._chologSkip = true;
      }

      self.originalXhrOpen.apply(this, arguments);
    };

    XMLHttpRequest.prototype.send = function (body?: Document | XMLHttpRequestBodyInit | null) {
      if ((this as any)._chologSkip) {
        return self.originalXhrSend.apply(this, arguments);
      }

      const xhr = this as XMLHttpRequest & {
        _chologMethod?: string;
        _chologUrl?: string;
        _chologStartTime?: number;
        _chologLogged?: boolean;
      };
      xhr._chologStartTime = Date.now();

      const requestId = RequestContext.getCurrentRequestId() || RequestContext.startNewRequest();
      // X-Request-ID 헤더 설정 (이미 설정되어 있지 않은 경우)
      this.setRequestHeader("X-Request-ID", requestId);

      const requestDetails: LogHttp = {
        requestMethod: (xhr._chologMethod || "UnknownMethod").toUpperCase(),
        requestUri: xhr._chologUrl || "UnknownURL",
      };

      // 요청 시작 로그 (선택적)
      // Logger.logHttp(`XHR Request START: ${requestDetails.method} ${requestDetails.requestUri}`, { request: requestDetails });

      const onLoadEnd = () => {
        if (xhr._chologLogged) return; // 이미 로깅된 경우 중복 방지
        xhr._chologLogged = true;

        const responseTime = xhr._chologStartTime ? Date.now() - xhr._chologStartTime : undefined;
        let errorDetails: LogError | undefined = undefined;

        if (xhr.status === 0 || xhr.status >= 400) {
          errorDetails = {
            type: xhr.statusText || "XHRError",
            message: `XHR 오류 => ${requestDetails.requestUri} - 상태 => ${xhr.status || "N/A"}. ReadyState: ${
              xhr.readyState
            }`,
          };
        }

        Logger.logHttp(
          `XHR 요청 => ${requestDetails.requestMethod} ${requestDetails.requestUri} - 상태 => ${xhr.status}`,
          {
            ...requestDetails,
            httpStatus: xhr.status,
            responseTime,
          },
          undefined, // clientDetails
          errorDetails
        );

        // 이벤트 리스너 제거
        xhr.removeEventListener("loadend", onLoadEnd);
        // xhr.removeEventListener("error", onLoadEnd); // loadend가 error, abort, timeout도 커버
        // xhr.removeEventListener("abort", onLoadEnd);
        // xhr.removeEventListener("timeout", onLoadEnd);
      };

      // 'loadend'는 load, error, abort, timeout 이후에 발생하므로 대부분의 케이스를 커버.
      xhr.addEventListener("loadend", onLoadEnd);

      self.originalXhrSend.apply(this, arguments);
    };
  }

  public static init(): void {
    if (this.isInitialized) return;
    try {
      this.patchFetch();
      this.patchXMLHttpRequest();
      this.isInitialized = true;
    } catch (error) {
      console.error("[CHO:LOG] SDK: NetworkInterceptor 초기화에 실패했습니다.", error);
    }
  }
}
