// 기본 API 응답 타입
interface BaseResponse {
  success: boolean;
  timestamp: string;
  error?: {
    code: "INVALID_REQUEST" | "UNAUTHORIZED" | "NOT_FOUND" | "INTERNAL_ERROR";
    message: string;
  };
}

// 로그 레벨 통계 타입
export interface LogStats {
  TRACE: number;
  DEBUG: number;
  INFO: number;
  WARN: number;
  ERROR: number;
  FATAL: number;
}

// 로그 통계 응답 타입
export interface LogStatsResponse extends BaseResponse {
  data: {
    projectId: number;
    stats: LogStats;
  };
}

// 로그 상태 타입
export interface LogState {
  isLoading: boolean;
  error: BaseResponse["error"] | null;
  stats: LogStats | null;
  projectId: number | null;
}

// 에러 통계 항목 타입
export interface ErrorStatItem {
  errorName: string;
  errorCode: string;
  count: number;
}

// 에러 통계 조회 요청 파라미터
export interface ErrorStatsRequest {
  projectId: number;
  startDate?: string;
  endDate?: string;
}

// 에러 통계 응답 타입
export interface ErrorStatsResponse extends BaseResponse {
  data: ErrorStatItem[];
}

// LogState에 errorStats 추가
export interface LogState {
  isLoading: boolean;
  error: BaseResponse["error"] | null;
  stats: LogStats | null;
  projectId: number | null;
  errorStats: ErrorStatItem[]; // 추가
}

// 에러 타임라인 항목 타입
export interface ErrorTimelineItem {
  timestamp: string;
  errorCount: number;
}

// 에러 타임라인 조회 요청 파라미터
export interface ErrorTimelineRequest {
  projectId: number;
  startDate?: string;
  endDate?: string;
}

// 에러 타임라인 응답 타입
export interface ErrorTimelineResponse extends BaseResponse {
  data: ErrorTimelineItem[];
}

// 에러 유형별 통계 항목 타입
export interface ErrorTypeRatioItem {
  errorType: string;
  count: number;
  ratio: number;
}

// 에러 유형별 통계 요청 파라미터
export interface ErrorTypeRatioRequest {
  projectId: number;
  startDate?: string;
  endDate?: string;
}

// 에러 유형별 통계 응답 타입
export interface ErrorTypeRatioResponse extends BaseResponse {
  data: ErrorTypeRatioItem[];
}

// 에러 추세 항목 타입
export interface ErrorTrendItem {
  period: string;
  errorCount: number;
}

// 에러 추세 조회 요청 파라미터
export interface ErrorTrendRequest {
  projectId: number;
  startDate?: string;
  endDate?: string;
  interval?: "day" | "week" | "month";
}

// 에러 추세 응답 타입
export interface ErrorTrendResponse extends BaseResponse {
  data: ErrorTrendItem[];
}

// LogState 업데이트
export interface LogState {
  isLoading: boolean;
  error: BaseResponse["error"] | null;
  stats: LogStats | null;
  projectId: number | null;
  errorStats: ErrorStatItem[];
  errorTimeline: ErrorTimelineItem[];
  errorTypeRatios: ErrorTypeRatioItem[];
  errorTrends: ErrorTrendItem[]; // 추가
  logs: LogDetail[]; // 추가
  traceLogs: LogDetail[]; // Add this line
  pagination: {
    pageNumber: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    first: boolean;
    last: boolean;
  } | null; // 추가
}

// 로그 상세 정보 타입
export interface LogDetail {
  _id: string;
  timestamp: string;
  message: string;
  apiPath: string;
  level: "TRACE" | "DEBUG" | "INFO" | "WARN" | "ERROR" | "FATAL";
  traceId: string;
  spanId: string;
  details: {
    errorCode?: string;
    stackTrace?: string;
    error?: {
      name: string;
      message: string;
      stackTrace: string;
    };
    userId?: string;
    ip?: string;
    [key: string]: unknown;
  };
}

export interface LogDetailResponse {
  success: boolean;
  data: LogDetail | null;
  error?: {
    code: string;
    message: string;
  };
  timestamp: string;
}

// 로그 조회 요청 파라미터
export interface LogListRequest {
  projectId: number;
  page?: number;
  size?: number;
  sort?: string;
}

// 페이지네이션 정보를 포함한 응답 데이터
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

// LogState에 logs 추가
export interface LogState {
  isLoading: boolean;
  error: BaseResponse["error"] | null;
  stats: LogStats | null;
  projectId: number | null;
  errorStats: ErrorStatItem[];
  errorTimeline: ErrorTimelineItem[];
  errorTypeRatios: ErrorTypeRatioItem[];
  errorTrends: ErrorTrendItem[];
  logs: LogDetail[]; // 추가
  pagination: {
    pageNumber: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    first: boolean;
    last: boolean;
  } | null; // 추가
  logDetail: LogDetail | null;
}

// 로그 검색 요청 파라미터
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
}

// 로그 검색 응답 타입 (LogListResponse와 동일한 구조 사용)
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

// API Path별 로그 조회 요청 파라미터
export interface LogByApiPathRequest {
  projectId: number;
  apiPath: string;
  page?: number;
  size?: number;
  sort?: string;
}

// API Path별 로그 응답 타입 (LogListResponse와 동일한 구조 사용)
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

// Trace 로그 조회 요청 파라미터
export interface TraceLogRequest {
  projectId: number;
  traceId: string;
}

// Trace 로그 응답 타입
export interface TraceLogResponse extends BaseResponse {
  data: LogDetail[];
}

// Archive Log Request Body
export interface ArchiveLogRequest {
  archiveReason: string;
}

// Archive Log Response Data
export interface ArchiveLogResponseData {
  logId: string;
  archiveStatus: string;
  archiveReason: string;
  timestamp: string;
}

// Archive Log Response
export interface ArchiveLogResponse extends BaseResponse {
  data: ArchiveLogResponseData;
}

export interface ArchiveLogRequest {
  logId: string;
  archiveReason: string;
}

// Update LogDetail interface to include archive status
export interface LogDetail {
  _id: string;
  timestamp: string;
  message: string;
  apiPath: string;
  level: "TRACE" | "DEBUG" | "INFO" | "WARN" | "ERROR" | "FATAL";
  traceId: string;
  spanId: string;
  archiveStatus?: string; // Add archive status
  archiveReason?: string; // Add archive reason
  details: {
    errorCode?: string;
    stackTrace?: string;
    error?: {
      name: string;
      message: string;
      stackTrace: string;
    };
    userId?: string;
    ip?: string;
    [key: string]: unknown;
  };
}

// Add these types to your existing log.types.ts file

// Archived Log Item Type
export interface ArchivedLogItem {
  logId: string;
  timestamp: string;
  apiPath: string;
  level: "TRACE" | "DEBUG" | "INFO" | "WARN" | "ERROR" | "FATAL";
  archiveReason: string;
  archivedAt: string;
}

// Archived Logs Request Parameters
export interface ArchivedLogsRequest {
  projectId: string;
  page?: number;
  size?: number;
}

// Archived Logs Response Data
export interface ArchivedLogsResponseData {
  content: ArchivedLogItem[];
  pageNumber: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  first: boolean;
  last: boolean;
}

// Archived Logs Response Type
export interface ArchivedLogsResponse extends BaseResponse {
  data: ArchivedLogsResponseData;
}

// Update LogState to include archived logs if needed
export interface LogState {
  isLoading: boolean;
  error: BaseResponse["error"] | null;
  stats: LogStats | null;
  projectId: number | null;
  errorStats: ErrorStatItem[];
  errorTimeline: ErrorTimelineItem[];
  errorTypeRatios: ErrorTypeRatioItem[];
  errorTrends: ErrorTrendItem[];
  logs: LogDetail[]; // 추가
  pagination: {
    pageNumber: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    first: boolean;
    last: boolean;
  } | null; // 추가
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
