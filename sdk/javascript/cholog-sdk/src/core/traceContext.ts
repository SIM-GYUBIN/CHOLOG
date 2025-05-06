// src/core/traceContext.ts (예시)
export class TraceContext {
  private static currentTraceId: string | null = null;
  private static currentSpanId: string | null = null; // 선택적: 스팬 개념 도입 시

  public static startNewTrace(): string {
    this.currentTraceId = this.generateId("trace");
    this.currentSpanId = null; // 새 트레이스 시작 시 스팬 초기화
    return this.currentTraceId;
  }

  // 필요시 Span ID도 유사하게 관리
  // public static startNewSpan(parentId?: string): string {
  //     this.currentSpanId = this.generateId('span');
  //     // parentId를 사용하여 부모-자식 관계 설정 가능
  //     return this.currentSpanId;
  // }

  public static getCurrentTraceId(): string | null {
    return this.currentTraceId;
  }

  public static setCurrentTraceId(traceId: string): void {
    this.currentTraceId = traceId;
  }

  private static generateId(prefix: string): string {
    if (crypto && crypto.randomUUID) {
      return crypto.randomUUID();
    }
    return `<span class="math-inline">\{prefix\}\-</span>{Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
  }
}
