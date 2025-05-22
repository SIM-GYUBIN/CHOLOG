/**
 * @description 로그 관련 타입 정의 모음
 * @author Cholog FE Team
 */

/**
 * @description 기본 API 응답 타입
 * @property {boolean} success - API 호출 성공 여부
 * @property {string} timestamp - API 응답 시간
 * @property {object} error - 에러 정보 (선택적)
 */
interface BaseResponse {
  success: boolean;
  timestamp: string;
  error?: {
    code: "INVALID_REQUEST" | "UNAUTHORIZED" | "NOT_FOUND" | "INTERNAL_ERROR";
    message: string;
  };
}

/**
 * [#LOG-1]
 * @description 로그 레벨별 통계 정보
 * @property {number} total - 전체 로그 수
 * @property {number} trace - 추적 레벨 로그 수
 * @property {number} debug - 디버그 레벨 로그 수
 * @property {number} info - 정보 레벨 로그 수
 * @property {number} warn - 경고 레벨 로그 수
 * @property {number} error - 에러 레벨 로그 수
 * @property {number} fatal - 치명적 에러 레벨 로그 수
 */
export interface LogStats {
  total: number;
  trace: number;
  debug: number;
  info: number;
  warn: number;
  error: number;
  fatal: number;
}

/**
 * [#LOG-1]
 * @description 로그 통계 API 응답 타입
 * @extends {BaseResponse}
 * @property {LogStats} data - 로그 레벨별 통계 데이터
 */
export interface LogStatsResponse extends BaseResponse {
  data: LogStats;
}

/**
 * [#LOG-2]
 * @description 에러 통계 항목 타입
 * @property {string} errorName - 에러 이름
 * @property {string} errorCode - 에러 코드
 * @property {number} count - 발생 횟수
 */
export interface ErrorStatItem {
  errorName: string;
  errorCode: string;
  count: number;
}

/**
 * [#LOG-2]
 * @description 에러 통계 조회를 위한 요청 파라미터
 * @property {number} projectId - 프로젝트 식별자
 * @property {string} startDate - 조회 시작 날짜 (선택적)
 * @property {string} endDate - 조회 종료 날짜 (선택적)
 */
export interface ErrorStatsRequest {
  projectId: number;
  startDate?: string;
  endDate?: string;
}

/**
 * [#LOG-2]
 * @description 에러 통계 응답 타입
 * @extends {BaseResponse}
 * @property {object} data - 에러 통계 항목 타입
 */
export interface ErrorStatsResponse extends BaseResponse {
  data: ErrorStatItem[];
}

/**
 * [#LOG-3]
 * @description 에러 타임라인 항목 타입
 * @property {string} timestamp - 에러 발생 시간
 * @property {number} errorCount - 해당 시점의 에러 발생 횟수
 */
export interface ErrorTimelineItem {
  timestamp: string;
  logCount: number;
}

/**
 * [#LOG-3]
 * @description 에러 타임라인 조회를 위한 요청 파라미터
 * @property {number} projectId - 프로젝트 식별자
 * @property {string} startDate - 조회 시작 날짜 (선택적)
 * @property {string} endDate - 조회 종료 날짜 (선택적)
 */
export interface ErrorTimelineRequest {
  projectId: number;
  startDate?: string;
  endDate?: string;
}

/**
 * [#LOG-3]
 * @description 에러 타임라인 응답 타입
 * @extends {BaseResponse}
 * @property {object} data - 에러 타임라인 항목 타입
 */
export interface ErrorTimelineResponse extends BaseResponse {
  data: ErrorTimelineItem[];
}

/**
 * [#LOG-4]
 * @description 에러 유형별 통계 항목 타입
 * @property {string} errorType - 에러 유형
 * @property {number} count - 발생 횟수
 * @property {number} ratio - 전체 대비 비율
 */
export interface ErrorTypeRatioItem {
  errorType: string;
  count: number;
  ratio: number;
}

/**
 * [#LOG-4]
 * @description 에러 유형별 통계 조회를 위한 요청 파라미터
 * @property {number} projectId - 프로젝트 식별자
 * @property {string} startDate - 조회 시작 날짜 (선택적)
 * @property {string} endDate - 조회 종료 날짜 (선택적)
 */
export interface ErrorTypeRatioRequest {
  projectId: number;
  startDate?: string;
  endDate?: string;
}

/**
 * [#LOG-4]
 * @description 에러 유형별 통계 응답 타입
 * @extends {BaseResponse}
 * @property {object} data - 에러 유형별 통계 항목 타입
 */
export interface ErrorTypeRatioResponse extends BaseResponse {
  data: ErrorTypeRatioItem[];
}

/**
 * [#LOG-5]
 * @description 에러 추세 항목 타입
 * @property {string} period - 기간 정보
 * @property {number} errorCount - 해당 기간의 에러 발생 횟수
 */
export interface ErrorTrendItem {
  period: string;
  errorCount: number;
}

/**
 * [#LOG-5]
 * @description 에러 추세 조회를 위한 요청 파라미터
 * @property {number} projectId - 프로젝트 식별자
 * @property {string} startDate - 조회 시작 날짜 (선택적)
 * @property {string} endDate - 조회 종료 날짜 (선택적)
 * @property {string} interval - 조회 간격 (day/week/month)
 */
export interface ErrorTrendRequest {
  projectId: number;
  startDate?: string;
  endDate?: string;
  interval?: "day" | "week" | "month";
}

/**
 * [#LOG-5]
 * @description 에러 추세 응답 타입
 * @extends {BaseResponse}
 * @property {object} data - 에러 추세 항목 타입
 */
export interface ErrorTrendResponse extends BaseResponse {
  data: ErrorTrendItem[];
}

/**
 * [#LOG-10]
 * @description 로그 상세 조회를 위한 요청 파라미터
 * @property {number} projectId - 프로젝트 식별자
 * @property {string} logId - 로그 고유 식별자
 */
export interface LogDetailRequest {
  projectId: number;
  logId: string;
}

/**
 * [#LOG-10]
 * @description 로그 상세 정보 타입
 * @property {string} id - 로그 고유 식별자 - 필수
 * @property {string} timestamp - 로그 발생 시간 - 필수
 * @property {string} level - 로그 레벨 - 필수
 * @property {string} message - 로그 메시지 - 필수
 * @property {string} source - 로그 소스 - 필수
 * @property {string} projectKey - 프로젝트 키 - 필수
 * @property {string} environment - 환경 - 필수
 * @property {string} traceId - 추적 ID
 * @property {string} logger - 로거 이름
 * @property {string} logType - 로그 타입
 * @property {object} http - HTTP 관련 정보
 * @property {object} error - 에러 정보
 * @property {object} payload - 추가 데이터
 */
export interface LogDetail {
  id: string;
  timestamp: string;
  level: string;
  message: string;
  source: "frontend" | "backend";
  projectKey: string;
  environment: string;
  traceId: string;
  logger: string;
  logType: string;
  client?: {
    url?: string;
    userAgent?: string;
    referrer?: string;
  };
  http?: {
    request?: {
      method?: string;
      url?: string;
      [key: string]: any;
    };
    response?: any; // string 또는 객체 모두 허용
    durationMs?: number;
  };
  error?: {
    type: string;
    message: string;
    stacktrace?: string;
  };
  event?: {
    type: string;
    targetSelector?: string;
    properties?: Record<string, any>;
  };
  payload?: Record<string, any>;
}

// API 응답 전체 타입
export interface LogDetailResponse {
  success: boolean;
  data: LogDetail;
  timestamp: string;
}

/**
 * @description 로그 목록 응답 타입
 * @extends {BaseResponse}
 * @property {object} data - 로그 목록 데이터
 */
export interface LogListResponse extends BaseResponse {
  data: {
    content: LogDetail[];
    pageNumber: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    first: boolean;
    last: boolean;
  };
}

/**
 * [#LOG-6]
 * @description 프로젝트 로그 리스트 조회를 위한 요청 파라미터
 * @property {number} projectId - 프로젝트 식별자
 * @property {number} page - 페이지 번호 (선택적)
 * @property {number} size - 페이지 크기 (선택적)
 */
export interface LogListRequest {
  projectId: number;
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * [#LOG-6]
 * @description 프로젝트 로그 리스트 응답 타입
 * @extends {BaseResponse}
 * @property {object} data - 로그 리스트 데이터
 */
export interface LogListResponse extends BaseResponse {
  data: {
    content: LogDetail[];
    pageNumber: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    first: boolean;
    last: boolean;
  };
}

/**
 * [#LOG-7]
 * @description 로그 검색 조회를 위한 요청 파라미터
 * @property {number} projectId - 프로젝트 식별자
 * @property {string} logId - 로그 고유 식별자
 * @property {string} level - 로그 레벨 (선택적)
 * @property {string} apiPath - API 경로 (선택적)
 * @property {string} message - 로그 메시지 (선택적)
 * @property {string} traceId - 추적 ID (선택적)
 * @property {string} spanId - 스팬 ID (선택적)
 */
export interface LogSearchRequest {
  projectId: number;
  page?: number;
  size?: number;
  sort?: string;
  level?: "TRACE" | "DEBUG" | "INFO" | "WARN" | "ERROR" | "FATAL";
  apiPath?: string;
  message?: string;
  traceId?: string;
  spanId?: string;
  source?: "frontend" | "backend"; // 개발 환경 필터 추가
}

/**
 * [#LOG-7]
 * @description 로그 검색 응답 타입
 * @extends {BaseResponse}
 * @property {object} data - 로그 상세 정보 데이터
 * @property {object} data.logs - 로그 리스트 데이터
 * @property {object} data.pagination - 페이지네이션 정보
 * @property {number} data.pagination.pageNumber - 현재 페이지 번호
 * @property {number} data.pagination.totalPages - 전체 페이지 수
 * @property {number} data.pagination.totalElements - 전체 로그 수
 * @property {number} data.pagination.pageSize - 페이지 크기
 * @property {boolean} data.pagination.first - 첫 번째 페이지 여부
 * @property {boolean} data.pagination.last - 마지막 페이지 여부
 */
export interface LogSearchResponse extends BaseResponse {
  data: {
    content: LogDetail[];
    pageNumber: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    first: boolean;
    last: boolean;
  };
}

/**
 * [#LOG-8]
 * @description API 경로별 로그 조회를 위한 요청 파라미터
 * @property {number} projectId - 프로젝트 식별자
 * @property {string} logId - 로그 고유 식별자
 * @property {string} level - 로그 레벨 (선택적)
 */
export interface LogByApiPathRequest {
  projectId: number;
  apiPath: string;
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * [#LOG-8]
 * @description API 경로별 로그 응답 타입
 * @extends {BaseResponse}
 * @property {object} data - 로그 리스트 데이터
 * @property {object} data.pagination - 페이지네이션 정보
 * @property {number} data.pagination.pageNumber - 현재 페이지 번호
 * @property {number} data.pagination.totalPages - 전체 페이지 수
 * @property {number} data.pagination.totalElements - 전체 로그 수
 * @property {number} data.pagination.pageSize - 페이지 크기
 * @property {boolean} data.pagination.first - 첫 번째 페이지 여부
 * @property {boolean} data.pagination.last - 마지막 페이지 여부
 */
export interface LogByApiPathResponse extends BaseResponse {
  data: {
    content: {
      traceId: string;
      timestamp: string;
      apiPath: string;
      level: "TRACE" | "DEBUG" | "INFO" | "WARN" | "ERROR" | "FATAL";
      message: string;
    }[];
    pageNumber: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    first: boolean;
    last: boolean;
  };
}

/**
 * [#LOG-9]
 * @description Trace ID 별 로그 조회를 위한 요청 파라미터
 * @property {number} projectId - 프로젝트 식별자
 * @property {string} traceId - 추적 ID
 */
export interface TraceLogRequest {
  projectId: number;
  traceId: string;
}

/**
 * [#LOG-9]
 * @description Trace ID 별 로그 응답 타입
 * @extends {BaseResponse}
 * @property {object} data - 로그 리스트 데이터
 */
export interface TraceLogResponse extends BaseResponse {
  data: LogDetail[];
}

/**
 * [#LOG-10]
 * @description 특정 로그 상세 정보 조회를 위한 요청 파라미터
 * @property {success} success - API 호출 성공 여부
 * @property {data} data - 로그 상세 정보 데이터
 * @property {error} error - 에러 정보 (선택적)
 * @property {timestamp} timestamp - API 응답 시간
 */
export interface LogDetailResponse {
  success: boolean;
  data: LogDetail | null;
  error?: {
    code: string;
    message: string;
  };
  timestamp: string;
}

/**
 * [#LOG-11]
 * @description 로그 아카이브 요청 파라미터
 * @property {string} logId - 아카이브할 로그 ID
 * @property {string} archiveReason - 아카이브 이유
 */
export interface ArchiveLogRequest {
  logId: string;
  archiveReason: string;
}

/**
 * [#LOG-11]
 * @description 로그 아카이브 응답 데이터
 * @property {string} logId - 아카이브된 로그 ID
 * @property {string} archiveStatus - 아카이브 상태
 * @property {string} archiveReason - 아카이브 이유
 * @property {string} timestamp - 아카이브 시간
 */
export interface ArchiveLogResponseData {
  logId: string;
  archiveStatus: string;
  archiveReason: string;
  timestamp: string;
}

/**
 * [#LOG-11]
 * @description 로그 아카이브 응답
 * @extends {BaseResponse}
 * @property {ArchiveLogResponseData} data - 아카이브 응답 데이터
 */
export interface ArchiveLogResponse extends BaseResponse {
  data: ArchiveLogResponseData;
}

/**
 * [#LOG-12]
 * @description 아카이브된 로그 아이템 타입
 * @property {string} logId - 로그 ID
 * @property {string} nickname - 아카이브한 사용자 닉네임
 * @property {string} memo - 아카이브 메모
 * @property {string} logLevel - 로그 레벨
 * @property {string} logSource - 로그 소스
 * @property {string} logType - 로그 타입
 * @property {string} logEnvironment - 로그 환경
 * @property {string} logMessage - 로그 메시지
 * @property {string} logTimestamp - 로그 발생 시간
 */
export interface ArchivedLogItem {
  logId: string;
  nickname: string;
  memo: string;
  logLevel: "TRACE" | "DEBUG" | "INFO" | "WARN" | "ERROR" | "FATAL";
  logSource: "frontend" | "backend";
  logType: string;
  logEnvironment: string;
  logMessage: string;
  logTimestamp: string;
}

/**
 * [#LOG-12]
 * @description 아카이브된 로그 조회를 위한 요청 파라미터
 * @property {string} projectId - 프로젝트 ID
 * @property {number} page - 페이지 번호 (선택적)
 * @property {number} size - 페이지 크기 (선택적)
 */
export interface ArchivedLogsRequest {
  projectId: string;
  page?: number;
  size?: number;
}

/**
 * [#LOG-12]
 * @description 아카이브된 로그 응답 데이터
 * @property {ArchivedLogItem[]} content - 아카이브된 로그 목록
 * @property {number} pageNumber - 현재 페이지 번호
 * @property {number} totalPages - 전체 페이지 수
 */
export interface ArchivedLogsResponseData {
  content: ArchivedLogItem[];
  pageNumber: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  first: boolean;
  last: boolean;
}

/**
 * [#LOG-12]
 * @description 아카이브된 로그 응답
 * @extends {BaseResponse}
 * @property {ArchivedLogsResponseData} data - 아카이브된 로그 응답 데이터
 */
export interface ArchivedLogsResponse extends BaseResponse {
  data: ArchivedLogsResponseData;
}

/**
 * @description 로그 상태 관리를 위한 Redux 상태 타입
 * @property {boolean} isLoading - 데이터 로딩 상태
 * @property {object | null} error - 에러 정보
 * @property {LogStats | null} stats - 로그 통계 정보
 * @property {number | null} projectId - 현재 선택된 프로젝트 ID
 * @property {ErrorStatItem[]} errorStats - 에러 통계 항목 배열
 * @property {ErrorTimelineItem[]} errorTimeline - 에러 타임라인 항목 배열
 * @property {ErrorTypeRatioItem[]} errorTypeRatios - 에러 유형별 통계 항목 배열
 * @property {ErrorTrendItem[]} errorTrends - 에러 추세 항목 배열
 * @property {LogDetail[]} logs - 로그 상세 정보 배열
 * @property {LogDetail[]} traceLogs - 추적 로그 배열
 * @property {object | null} pagination - 페이지네이션 정보
 * @property {LogDetail | null} logDetail - 현재 선택된 로그 상세 정보
 * @property {ArchiveLogResponseData | null} archiveResult - 아카이브 결과 정보
 * @property {ArchivedLogsResponseData | null} archivedLogs - 아카이브된 로그 목록
 */
export interface LogState {
  isLoading: boolean;
  error: BaseResponse["error"] | null;
  stats: LogStats | null;
  projectId: number | null;
  errorStats: ErrorStatItem[];
  errorTimeline: ErrorTimelineItem[];
  errorTypeRatios: ErrorTypeRatioItem[];
  errorTrends: ErrorTrendItem[];
  logs: LogDetail[];
  traceLogs: LogDetail[];
  pagination: {
    pageNumber: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    first: boolean;
    last: boolean;
  } | null;
  logDetail: LogDetail | null;
  archiveResult: ArchiveLogResponseData | null;
  archivedLogs: ArchivedLogsResponseData | null;
}

// Error Codes for this endpoint
export type ArchivedLogsErrorCode =
  | "INVALID_REQUEST"
  | "UNAUTHORIZED"
  | "PROJECT_NOT_FOUND"
  | "INTERNAL_ERROR";
