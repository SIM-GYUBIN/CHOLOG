// src/core/eventTracker.ts
import { Logger } from "./logger";
import { RequestContext } from "./requestContext";
import {
  LogEvent,
  LogClient, // LogClient는 Logger가 자동 수집하므로 여기서 명시적 전달은 선택적
} from "../types";

export class EventTracker {
  private static config = {
    significantElementSelector: 'button, a, [role="button"], input[type="submit"], [data-cholog-action]',
  };

  public static init(options?: { significantElementSelector?: string }): void {
    if (typeof window === "undefined") return;

    if (options?.significantElementSelector) {
      this.config.significantElementSelector = options.significantElementSelector;
    }

    this.logNavigation(window.location.href, "initial_load");

    window.addEventListener("hashchange", () => this.logNavigation(window.location.href, "hash_change"));
    // SPA 라우터 변경 감지를 위해 popstate 외에도 pushState, replaceState 래핑 고려 (더 복잡)
    window.addEventListener("popstate", () => this.logNavigation(window.location.href, "popstate_navigation"));

    document.addEventListener(
      "click",
      (event) => {
        const targetElement = event.target as Element;
        const closestSignificantElement = targetElement.closest(this.config.significantElementSelector);

        if (closestSignificantElement) {
          RequestContext.startNewRequest(); // 클릭 시 새 요청 시작
          const eventDetails: LogEvent = {
            type: "user_interaction_click", // 또는 "significant_click"
            targetSelector: this.getElementPath(closestSignificantElement),
            properties: {
              // textContent는 개인정보 포함 가능성 있어 주의
              // elementText: closestSignificantElement.textContent?.trim().substring(0, 50) || "",
              elementType: closestSignificantElement.tagName.toLowerCase(),
              elementId: closestSignificantElement.id || undefined,
              elementClasses: closestSignificantElement.className || undefined,
            },
          };
          // Logger.logEvent 호출 시 clientDetails는 Logger가 자동 수집
          Logger.logEvent(`클릭 이벤트 => ${eventDetails.targetSelector}`, eventDetails);
        }
      },
      true // Use capture phase
    );
  }

  private static logNavigation(url: string, navigationType: string): void {
    // 페이지 로드/네비게이션은 새로운 컨텍스트로 간주하여 새 Request ID 시작
    RequestContext.startNewRequest();
    const eventDetails: LogEvent = {
      type: navigationType, // 예: "initial_load", "spa_navigation"
      properties: { currentUrl: url },
    };
    Logger.logEvent(`네비게이션 이벤트 => ${navigationType} to ${url}`, eventDetails);
  }

  private static getElementPath(element: Element): string {
    const parts: string[] = [];
    let currentElement: Element | null = element;
    while (currentElement && currentElement.tagName) {
      let selector = currentElement.tagName.toLowerCase();
      if (currentElement.id) {
        selector += `#${currentElement.id}`;
        parts.unshift(selector);
        break;
      } else if (currentElement.classList && currentElement.classList.length > 0) {
        selector += `.${Array.from(currentElement.classList).join(".")}`;
      }
      // nth-child 로직은 복잡성을 증가시키므로, 우선 단순 선택자로 유지
      parts.unshift(selector);
      if (currentElement === document.body || parts.length >= 7) break; // 최대 깊이 제한
      currentElement = currentElement.parentElement;
    }
    return parts.join(" > ");
  }
}
