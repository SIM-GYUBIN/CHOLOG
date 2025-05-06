// src/core/eventTracker.ts
import { Logger } from "./logger";
import { TraceContext } from "./traceContext";
import { LogEvent, LogClient } from "../types";

export class EventTracker {
  private static config = {
    // 클릭 시 새 트레이스 시작 및 로깅은 중요한 요소에만 한정
    // 아래 선택자는 예시이며, 사용자가 커스터마이징 가능하도록 init 옵션으로 받는 것이 좋음
    significantElementSelector: 'button, a, [role="button"], input[type="submit"], [data-cholog-action]',
  };

  public static init(options?: { significantElementSelector?: string }): void {
    if (options?.significantElementSelector) {
      this.config.significantElementSelector = options.significantElementSelector;
    }

    // 페이지 로드/네비게이션 시 로깅
    this.logNavigation(window.location.href, "initial_load");

    // SPA 네비게이션 감지 (hashchange, popstate 등)
    // pushState/replaceState는 직접 감지 어려우므로, 라이브러리 사용 또는 애플리케이션에서 명시적 호출 필요
    window.addEventListener("hashchange", () => this.logNavigation(window.location.href, "hash_change"));
    // window.addEventListener("popstate", () => this.logNavigation(window.location.href, "popstate"));

    // 중요한 요소 클릭 시 로깅
    document.addEventListener(
      "click",
      (event) => {
        const targetElement = event.target as Element;
        if (targetElement.closest(this.config.significantElementSelector)) {
          // 중요한 요소 클릭 시 새로운 Trace 시작 및 이벤트 로깅
          const newTraceId = TraceContext.startNewTrace(); // 새로운 액션 시작으로 간주
          const eventDetails: LogEvent = {
            type: "user_action_start", // 또는 "significant_click"
            targetSelector: this.getElementPath(targetElement),
          };
          Logger.logEvent(`User action started: Click on ${eventDetails.targetSelector}`, eventDetails);
        }
        // "아무곳이나 클릭"하는 일반 클릭은 로깅하지 않음
      },
      true // 캡처 단계
    );
  }

  private static logNavigation(url: string, navigationType: string): void {
    TraceContext.startNewTrace(); // 페이지 이동/로드는 새로운 Trace로 간주
    const eventDetails: LogEvent = { type: navigationType }; // "initial_load", "hash_change" 등
    Logger.logEvent(`Navigation to ${url}`, eventDetails);
  }

  // getElementPath는 이전과 동일하게 유지
  private static getElementPath(element: Element): string {
    const parts: string[] = [];
    let currentElement: Element | null = element;
    while (currentElement && currentElement.tagName) {
      let selector = currentElement.tagName.toLowerCase();
      if (currentElement.id) {
        selector += `#${currentElement.id}`;
        parts.unshift(selector);
        break; // id가 있으면 그걸로 종료하는 것이 일반적
      } else if (currentElement.classList && currentElement.classList.length > 0) {
        selector += `.${Array.from(currentElement.classList).join(".")}`;
      }
      // 형제 요소 중 몇 번째인지 (nth-child) 추가하면 더 정확해지지만 복잡도 증가. 우선은 생략.
      parts.unshift(selector);
      currentElement = currentElement.parentElement;
      if (parts.length > 5) break; // 너무 길어지는 것 방지 (선택)
    }
    return parts.join(" > ");
  }
}
