/**
 * @description 리포트 관련 타입 정의 모음
 * @author Cholog FE Team
 */

/**
 * [#REPORT-1]
 * @description 리포트 조회 요청 파라미터
 * @property {number} projectId - 조회할 프로젝트 ID
 */
export interface ReportRequestParams {
  projectId: number;
}

/**
 * [#REPORT-2]
 * @description 리포트 조회 요청 바디
 * @property {string} startDate - 조회 시작일 (yyyy-MM-dd)
 * @property {string} endDate - 조회 종료일 (yyyy-MM-dd)
 */
export interface ReportRequestBody {
  startDate: string;
  endDate: string;
}

/**
 * [#REPORT-3]
 * @description 로그 레벨 분포 항목
 * @property {string} level - 로그 레벨 (INFO, ERROR 등)
 * @property {number} count - 발생 횟수
 * @property {number} percentage - 전체 대비 비율
 */
export interface LogLevelItem {
  level: string;
  count: number;
  percentage: number;
}

/**
 * [#REPORT-4]
 * @description 로그 레벨 분포 데이터
 * @property {LogLevelItem[]} distribution - 로그 분포 목록
 * @property {number} totalLogsInDistribution - 총 로그 수
 */
export interface LogLevelDistribution {
  distribution: LogLevelItem[];
  totalLogsInDistribution: number;
}

/**
 * [#REPORT-5]
 * @description 자주 발생하는 에러 항목
 * @property {number} rank - 순위
 * @property {string} errorIdentifier - 에러 구분자
 * @property {number} occurrenceCount - 발생 횟수
 * @property {string} sourceOrigin - 프론트/백 구분
 */
export interface TopErrorItem {
  rank: number;
  errorIdentifier: string;
  occurrenceCount: number;
  sourceOrigin: string;
}

/**
 * [#REPORT-6]
 * @description 느린 API 항목
 * @property {number} rank - 순위
 * @property {string} httpMethod - HTTP 메서드
 * @property {string} requestPath - 요청 경로
 * @property {number} averageResponseTimeMs - 평균 응답 시간 (ms)
 * @property {number} maxResponseTimeMs - 최대 응답 시간 (ms)
 * @property {number} totalRequests - 총 요청 수
 */
export interface SlowBackendApiItem {
  rank: number;
  httpMethod: string;
  requestPath: string;
  averageResponseTimeMs: number;
  maxResponseTimeMs: number;
  totalRequests: number;
}

/**
 * [#REPORT-7]
 * @description 리포트 상세 응답 데이터
 */
export interface Report {
  projectId: number;
  periodDescription: string;
  generatedAt: string;
  totalLogCounts: {
    overallTotal: number;
    frontendTotal: number;
    backendTotal: number;
  };
  logLevelDistribution: LogLevelDistribution;
  topErrors: TopErrorItem[];
  slowBackendApis: SlowBackendApiItem[];
}

/**
 * [#REPORT-8]
 * @description 리포트 조회 성공 응답
 */
export interface ReportSuccessResponse {
  success: true;
  data: Report;
  timestamp: string;
}

/**
 * [#REPORT-9]
 * @description 리포트 조회 실패 응답
 */
export interface ReportErrorResponse {
  success: false;
  data: Record<string, never>;
  error: {
    code: ErrorCode;
    message: string;
  };
  timestamp: string;
}

/**
 * [#REPORT-10]
 * @description API 에러 코드 정의
 */
export type ErrorCode =
  | "INVALID_REQUEST"
  | "UNAUTHORIZED"
  | "REPORT_NOT_FOUND"
  | "PROJECT_NOT_FOUND"
  | "INTERNAL_ERROR";

/**
 * [#REPORT-11]
 * @description 리포트 단일 조회 응답 통합 타입
 */
export type ReportResponse = ReportSuccessResponse | ReportErrorResponse;
