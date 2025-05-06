// src/core/eventTracker.ts
import { Logger } from "./logger";
import { TraceContext } from "./traceContext";

export class EventTracker {
  private static config = {
    newTraceOnClick: true, // 기본적으로 클릭 시 새 트레이스 시작 (옵션으로 제어 가능하게)
    clickTargetSelector: 'button, a, [role="button"]', // 어떤 요소 클릭 시 새 트레이스 시작할지 CSS 선택자로 정의
  };

  public static init(options?: {
    newTraceOnClick?: boolean;
    clickTargetSelector?: string;
  }): void {
    if (options?.newTraceOnClick !== undefined) {
      this.config.newTraceOnClick = options.newTraceOnClick;
    }
    if (options?.clickTargetSelector) {
      this.config.clickTargetSelector = options.clickTargetSelector;
    }

    this.startNewTraceForNavigation(window.location.href); // 페이지 로드 시

    window.addEventListener("hashchange", () =>
      this.startNewTraceForNavigation(window.location.href)
    );
    // window.addEventListener('popstate', () => this.startNewTraceForNavigation(window.location.href));

    document.addEventListener(
      "click",
      (event) => {
        if (!this.config.newTraceOnClick) {
          // 클릭 시 새 트레이스 시작 옵션이 꺼져있으면, 현재 트레이스 유지 (페이지 이동 외에는)
          if (!TraceContext.getCurrentTraceId()) TraceContext.startNewTrace(); // 현재 Trace가 없다면 시작
          this.logGeneralClick(event.target as Element);
          return;
        }

        // 클릭 시 새 트레이스를 시작하는 로직
        const targetElement = event.target as Element;
        // 설정된 선택자(clickTargetSelector)에 해당하는 요소를 클릭했는지 확인
        // targetElement.closest()는 클릭된 요소 자신 또는 가장 가까운 조상이 선택자와 일치하는지 확인
        if (targetElement.closest(this.config.clickTargetSelector)) {
          // 의미있는 요소(버튼, 링크 등)를 클릭했다고 판단 -> 새로운 Trace 시작
          TraceContext.startNewTrace();
          Logger.info(`Action started: Click on interactive element`, {
            traceId: TraceContext.getCurrentTraceId(),
            eventType: "user.action.start",
            element: this.getElementPath(targetElement),
          });
        } else {
          // !!! 의미없는 요소 클릭 (예: 페이지 배경) -> 현재 Trace 유지 또는 무시
          if (!TraceContext.getCurrentTraceId()) TraceContext.startNewTrace(); // 현재 Trace가 없다면 시작
          this.logGeneralClick(targetElement); // 일반 클릭 로그는 남길 수 있음
        }
      },
      true
    );
  }

  private static logGeneralClick(targetElement: Element): void {
    Logger.info(`User clicked (general)`, {
      traceId: TraceContext.getCurrentTraceId(),
      eventType: "ui.click",
      element: this.getElementPath(targetElement),
    });
  }

  private static startNewTraceForNavigation(url: string): void {
    const traceId = TraceContext.startNewTrace(); // 새로운 Trace 시작
    Logger.info(`Navigation to ${url}`, {
      traceId,
      eventType: "navigation",
      url: url,
    });
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
      } else if (
        currentElement.classList &&
        currentElement.classList.length > 0
      ) {
        selector += `.${Array.from(currentElement.classList).join(".")}`;
      }
      parts.unshift(selector);
      currentElement = currentElement.parentElement;
    }
    return parts.join(" > ");
  }
}
