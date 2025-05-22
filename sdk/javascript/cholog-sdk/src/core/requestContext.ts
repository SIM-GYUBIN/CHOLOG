// src/core/requestContext.ts (예시)
export class RequestContext {
  private static currentRequestId: string | null = null;
  // private static currentSpanId: string | null = null; // 스팬 개념 도입 시

  public static startNewRequest(): string {
    this.currentRequestId = this.generateId();
    // this.currentSpanId = null; // 새 트레이스 시작 시 스팬 초기화
    return this.currentRequestId;
  }

  // 필요시 Span ID도 유사하게 관리
  // public static startNewSpan(parentId?: string): string {
  //     this.currentSpanId = this.generateId('span');
  //     // parentId를 사용하여 부모-자식 관계 설정 가능
  //     return this.currentSpanId;
  // }

  public static getCurrentRequestId(): string | null {
    return this.currentRequestId;
  }

  public static setCurrentRequestId(requestId: string): void {
    this.currentRequestId = requestId;
  }

  private static generateId(): string {
    if (typeof crypto !== "undefined" && crypto.randomUUID) {
      return crypto.randomUUID();
    }
    // Fallback for environments without crypto.randomUUID (e.g., older browsers, some test environments)
    return `request-${Date.now()}-${Math.random().toString(36).substring(2, 11)}`;
  }
}
