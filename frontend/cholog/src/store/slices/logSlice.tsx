import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import api from "../../api/axios";
import {
  LogStatsResponse,
  LogState,
  ErrorStatsRequest,
  ErrorStatsResponse,
  ErrorTimelineRequest,
  ErrorTimelineResponse,
  ErrorTypeRatioRequest,
  ErrorTypeRatioResponse,
  ErrorTrendRequest,
  ErrorTrendResponse,
  LogListRequest,
  LogListResponse,
  LogSearchRequest,
  LogSearchResponse,
  LogByApiPathRequest,
  LogByApiPathResponse,
  TraceLogRequest,
  TraceLogResponse,
  LogDetailResponse,
  LogDetailRequest,
  ArchiveLogRequest,
  ArchiveLogResponse,
  ArchiveLogResponseData,
  ArchivedLogsRequest,
  ArchivedLogsResponse,
} from "../../types/log.types";

/**
 * ============================================
 * [#LOG-1]
 * [GET] /log/:projectId/stats
 * 프로젝트의 로그 레벨별 통계를 조회합니다.
 * --------------------------------------------
 * @param projectId - 조회 대상 프로젝트 ID
 * @returns LogStatsResponse
 * - TRACE, DEBUG, INFO, WARN, ERROR, FATAL 수치 포함
 * ============================================
 */
export const fetchLogStats = createAsyncThunk<LogStatsResponse, number>(
  "log/fetchStats",
  async (projectId, { rejectWithValue }) => {
    try {
      const response = await api.get<LogStatsResponse>(
        `/log/${projectId}/stats`,
        {
          headers: {
            "Content-Type": "application/json",
          },
        }
      );
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = "INTERNAL_ERROR";
      if (status === 400) errorCode = "INVALID_REQUEST";
      else if (status === 401) errorCode = "UNAUTHORIZED";

      return rejectWithValue({
        success: false,
        data: {
          total: 0,
          trace: 0,
          debug: 0,
          info: 0,
          warn: 0,
          error: 0,
          fatal: 0,
        },
        error: {
          code: errorCode,
          message:
            error.response?.data?.error?.message ||
            "로그 통계 조회 중 오류가 발생했습니다.",
        },
        timestamp: new Date().toISOString(),
      } as LogStatsResponse);
    }
  }
);

/**
 * ============================================
 * [#LOG-2]
 * [GET] /log/:projectId/errors/top3
 * 최근 에러 Top3를 조회합니다
 * --------------------------------------------
 * @param params - 프로젝트 ID, 조회 기간 (옵션)
 * @returns ErrorStatsResponse
 * - errorCode, errorName 별 발생 건수 포함
 * ============================================
 */
export const fetchErrorStats = createAsyncThunk<
  ErrorStatsResponse,
  ErrorStatsRequest
>("log/fetchErrorStats", async (params, { rejectWithValue }) => {
  try {
    const queryParams = new URLSearchParams();
    if (params.startDate) queryParams.append("startDate", params.startDate);
    if (params.endDate) queryParams.append("endDate", params.endDate);

    const response = await api.get<ErrorStatsResponse>(
      `/log/${params.projectId}/errors/top3?${queryParams.toString()}`,
      {
        headers: {
          "Content-Type": "application/json",
        },
      }
    );
    return response.data;
  } catch (error: any) {
    const status = error.response?.status;
    let errorCode = "INTERNAL_ERROR";
    if (status === 400) errorCode = "INVALID_REQUEST";
    else if (status === 401) errorCode = "UNAUTHORIZED";
    return rejectWithValue({
      success: false,
      data: [],
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "에러 통계 조회 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as ErrorStatsResponse);
  }
});

/**
 * ============================================
 * [#LOG-3]
 * [GET] /log/:projectId/errors/timeline
 * 프로젝트의 시간대별 에러 발생 추이를 조회합니다.
 * --------------------------------------------
 * @param params - 프로젝트 ID, 조회 시작일/종료일
 * @returns ErrorTimelineResponse
 * - timestamp 기준 각 구간별 errorCount 배열 반환
 * ============================================
 */
export const fetchErrorTimeline = createAsyncThunk<
  ErrorTimelineResponse,
  ErrorTimelineRequest
>("log/fetchErrorTimeline", async (params, { rejectWithValue }) => {
  try {
    const queryParams = new URLSearchParams();
    if (params.startDate) queryParams.append("startDate", params.startDate);
    if (params.endDate) queryParams.append("endDate", params.endDate);

    const response = await api.get<ErrorTimelineResponse>(
      `/log/${params.projectId}/timeline?${queryParams.toString()}`,
      {
        headers: {
          "Content-Type": "application/json",
        },
      }
    );
    return response.data;
  } catch (error: any) {
    const status = error.response?.status;
    let errorCode = "INTERNAL_ERROR";
    if (status === 400) errorCode = "INVALID_REQUEST";
    else if (status === 401) errorCode = "UNAUTHORIZED";
    return rejectWithValue({
      success: false,
      data: [],
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "에러 타임라인 조회 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as ErrorTimelineResponse);
  }
});

/**
 * ============================================
 * [#LOG-4]
 * [GET] /log/:projectId/errors/type-ratio
 * 프로젝트 에러의 유형별 비율 데이터를 조회합니다.
 * --------------------------------------------
 * @param params - 프로젝트 ID, 조회 기간 (옵션)
 * @returns ErrorTypeRatioResponse
 * - errorType 기준 비율 및 건수 포함
 * ============================================
 */
export const fetchErrorTypeRatio = createAsyncThunk<
  ErrorTypeRatioResponse,
  ErrorTypeRatioRequest
>("log/fetchErrorTypeRatio", async (params, { rejectWithValue }) => {
  try {
    const queryParams = new URLSearchParams();
    if (params.startDate) queryParams.append("startDate", params.startDate);
    if (params.endDate) queryParams.append("endDate", params.endDate);

    const response = await api.get<ErrorTypeRatioResponse>(
      `/log/${params.projectId}/errors/type-ratio?${queryParams.toString()}`,
      {
        headers: {
          "Content-Type": "application/json",
        },
      }
    );
    return response.data;
  } catch (error: any) {
    const status = error.response?.status;
    let errorCode = "INTERNAL_ERROR";
    if (status === 400) errorCode = "INVALID_REQUEST";
    else if (status === 401) errorCode = "UNAUTHORIZED";
    return rejectWithValue({
      success: false,
      data: [],
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "에러 유형별 통계 조회 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as ErrorTypeRatioResponse);
  }
});

/**
 * ============================================
 * [#LOG-5]
 * [GET] /log/:projectId/errors/trend
 * 프로젝트의 에러 발생 추세(일/주/월 단위)를 조회합니다.
 * --------------------------------------------
 * @param params - 프로젝트 ID, 기간, interval(day/week/month)
 * @returns ErrorTrendResponse
 * - 집계 단위(period)별 errorCount 수치를 반환
 * ============================================
 */
export const fetchErrorTrend = createAsyncThunk<
  ErrorTrendResponse,
  ErrorTrendRequest
>("log/fetchErrorTrend", async (params, { rejectWithValue }) => {
  try {
    const queryParams = new URLSearchParams();
    if (params.startDate) queryParams.append("startDate", params.startDate);
    if (params.endDate) queryParams.append("endDate", params.endDate);
    if (params.interval) queryParams.append("interval", params.interval);

    const response = await api.get<ErrorTrendResponse>(
      `/log/${params.projectId}/errors/trend?${queryParams.toString()}`,
      {
        headers: {
          "Content-Type": "application/json",
        },
      }
    );
    return response.data;
  } catch (error: any) {
    const status = error.response?.status;
    let errorCode = "INTERNAL_ERROR";
    if (status === 400) errorCode = "INVALID_REQUEST";
    else if (status === 401) errorCode = "UNAUTHORIZED";
    return rejectWithValue({
      success: false,
      data: [],
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "에러 추세 조회 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as ErrorTrendResponse);
  }
});

/**
 * ============================================
 * [#LOG-6]
 * [GET] /api/log/:projectId
 * 프로젝트의 로그 목록을 조회합니다.
 * --------------------------------------------
 * @param params - 프로젝트 ID, 페이지 정보
 * @returns LogListResponse
 * ============================================
 */
export const fetchLogs = createAsyncThunk<LogListResponse, LogListRequest>(
  "log/fetchLogs",
  async (params, { rejectWithValue }) => {
    try {
      const queryParams = new URLSearchParams();
      if (params.page) queryParams.append("page", params.page.toString());
      if (params.size) queryParams.append("size", params.size.toString());
      if (params.sort) queryParams.append("sort", params.sort);

      const response = await api.get<LogListResponse>(
        `/log/${params.projectId}?${queryParams.toString()}`,
        {
          headers: {
            "Content-Type": "application/json",
          },
        }
      );
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = "INTERNAL_ERROR";
      if (status === 400) errorCode = "INVALID_REQUEST";
      else if (status === 401) errorCode = "UNAUTHORIZED";

      return rejectWithValue({
        success: false,
        data: {
          content: [],
          pageNumber: 1,
          totalPages: 0,
          totalElements: 0,
          pageSize: 20,
          first: true,
          last: true,
        },
        error: {
          code: errorCode,
          message:
            error.response?.data?.error?.message ||
            "로그 목록 조회 중 오류가 발생했습니다.",
        },
        timestamp: new Date().toISOString(),
      } as LogListResponse);
    }
  }
);

/**
 * ============================================
 * [#LOG-7]
 * [GET] /api/log/:projectId/search
 * 프로젝트의 로그를 검색 조건에 따라 조회합니다.
 * --------------------------------------------
 * @param params - 프로젝트 ID, 페이지 정보, 검색 조건
 * @returns LogSearchResponse
 * ============================================
 */
export const searchLogs = createAsyncThunk<LogSearchResponse, LogSearchRequest>(
  "log/searchLogs",
  async (params, { rejectWithValue }) => {
    try {
      const queryParams = new URLSearchParams();
      if (params.page !== undefined)
        queryParams.append("page", params.page.toString());
      if (params.size) queryParams.append("size", params.size.toString());
      if (params.sort) queryParams.append("sort", params.sort);
      if (params.level) queryParams.append("level", params.level.toLowerCase()); // 소문자로 변환
      if (params.apiPath) queryParams.append("apiPath", params.apiPath);
      if (params.message) queryParams.append("message", params.message);
      if (params.source) queryParams.append("source", params.source); // frontend/backend 필터 추가

      const response = await api.get<LogSearchResponse>(
        `/log/${params.projectId}/search?${queryParams.toString()}`,

        {
          headers: {
            "Content-Type": "application/json",
          },
        }
      );
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = "INTERNAL_ERROR";
      if (status === 400) errorCode = "INVALID_REQUEST";
      else if (status === 401) errorCode = "UNAUTHORIZED";

      return rejectWithValue({
        success: false,
        data: {
          content: [],
          pageNumber: 0,
          totalPages: 0,
          totalElements: 0,
          pageSize: 20,
          first: true,
          last: true,
        },
        error: {
          code: errorCode,
          message:
            error.response?.data?.error?.message ||
            "로그 검색 중 오류가 발생했습니다.",
        },
        timestamp: new Date().toISOString(),
      } as LogSearchResponse);
    }
  }
);

/**
 * ============================================
 * [#LOG-8]
 * [GET] /api/log/:projectId/:apiPath
 * API 경로별 로그를 조회합니다.
 * --------------------------------------------
 * @param params - 프로젝트 ID, API 경로, 페이지 정보
 * @returns LogByApiPathResponse
 * ============================================
 */
export const fetchLogsByApiPath = createAsyncThunk<
  LogByApiPathResponse,
  LogByApiPathRequest
>("log/fetchLogsByApiPath", async (params, { rejectWithValue }) => {
  try {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined)
      queryParams.append("page", params.page.toString());
    if (params.size) queryParams.append("size", params.size.toString());
    if (params.sort) queryParams.append("sort", params.sort);

    const encodedApiPath = encodeURIComponent(params.apiPath);
    const response = await api.get<LogByApiPathResponse>(
      `/api/log/${params.projectId}/${encodedApiPath}?${queryParams.toString()}`,
      {
        headers: {
          "Content-Type": "application/json",
        },
      }
    );
    return response.data;
  } catch (error: any) {
    const status = error.response?.status;
    let errorCode = "INTERNAL_ERROR";
    if (status === 400) errorCode = "INVALID_REQUEST";
    else if (status === 401) errorCode = "UNAUTHORIZED";

    return rejectWithValue({
      success: false,
      data: {
        content: [],
        pageNumber: 0,
        totalPages: 0,
        totalElements: 0,
        pageSize: 20,
        first: true,
        last: true,
      },
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "API 경로별 로그 조회 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as LogByApiPathResponse);
  }
});

/**
 * ============================================
 * [#LOG-9]
 * [GET] /api/log/:projectId/trace/:traceId
 * Trace ID별 로그 흐름을 조회합니다.
 * --------------------------------------------
 * @param params - 프로젝트 ID, Trace ID
 * @returns TraceLogResponse
 * ============================================
 */
export const fetchTraceLog = createAsyncThunk<
  TraceLogResponse,
  TraceLogRequest
>("log/fetchTraceLog", async (params, { rejectWithValue }) => {
  try {
    const response = await api.get<TraceLogResponse>(
      `/log/${params.projectId}/trace/${params.traceId}`,
      {
        headers: {
          "Content-Type": "application/json",
        },
      }
    );
    return response.data;
  } catch (error: any) {
    const status = error.response?.status;
    let errorCode = "INTERNAL_ERROR";
    if (status === 400) errorCode = "INVALID_REQUEST";
    else if (status === 401) errorCode = "UNAUTHORIZED";

    return rejectWithValue({
      success: false,
      data: [],
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "Trace 로그 조회 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as TraceLogResponse);
  }
});

/**
 * ============================================
 * [#LOG-10]
 * [GET] /api/log/:projectId/detail/:logId
 * 특정 로그의 상세 정보를 조회합니다.
 * --------------------------------------------
 * @param params - 프로젝트 ID, 로그 ID
 * @returns LogDetailResponse
 * - 로그 상세 정보 포함
 * ============================================
 */
export const fetchLogDetail = createAsyncThunk<
  LogDetailResponse,
  LogDetailRequest
>("log/fetchLogDetail", async (params, { rejectWithValue }) => {
  try {
    const response = await api.get<LogDetailResponse>(
      `/log/${params.projectId}/detail/${params.logId}`,
      {
        headers: {
          "Content-Type": "application/json",
        },
      }
    );
    return response.data;
  } catch (error: any) {
    const status = error.response?.status;
    let errorCode = "INTERNAL_ERROR";

    if (status === 400) errorCode = "INVALID_REQUEST";
    else if (status === 401) errorCode = "UNAUTHORIZED";
    else if (status === 404) errorCode = "NOT_FOUND";

    return rejectWithValue({
      success: false,
      data: null,
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "로그 상세 정보 조회 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as LogDetailResponse);
  }
});

/**
 * ============================================
 * [#LOG-11]
 * [POST] /api/log/:projectId/archive
 * 로그를 아카이브 처리합니다.
 * --------------------------------------------
 * @param params - projectId, logId, archiveReason
 * @returns ArchiveLogResponse
 * ============================================
 */
export const archiveLog = createAsyncThunk<
  ArchiveLogResponse,
  { projectId: number } & ArchiveLogRequest
>("log/archiveLog", async (params, { rejectWithValue }) => {
  try {
    const response = await api.post<ArchiveLogResponse>(
      `/log/${params.projectId}/archive`,
      {
        logId: params.logId,
        archiveReason: params.archiveReason,
      },
      {
        headers: {
          "Content-Type": "application/json",
        },
      }
    );
    return response.data;
  } catch (error: any) {
    const status = error.response?.status;
    let errorCode = "INTERNAL_ERROR";

    if (status === 400) errorCode = "INVALID_REQUEST";
    else if (status === 401) errorCode = "UNAUTHORIZED";
    else if (status === 404) errorCode = "LOG_NOT_FOUND";

    return rejectWithValue({
      success: false,
      data: {
        logId: params.logId,
        archiveStatus: "FAILED",
        archiveReason: params.archiveReason,
        timestamp: new Date().toISOString(),
      },
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "로그 아카이브 처리 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as ArchiveLogResponse);
  }
});

/**
 * ============================================
 * [#LOG-12]
 * [GET] /api/log/:projectId/archive
 * 프로젝트의 아카이브된 로그 목록을 조회합니다.
 * --------------------------------------------
 * @param params - 프로젝트 ID, 페이지 정보
 * @returns ArchivedLogsResponse
 * ============================================
 */
export const fetchArchivedLogs = createAsyncThunk<
  ArchivedLogsResponse,
  ArchivedLogsRequest
>("log/fetchArchivedLogs", async (params, { rejectWithValue }) => {
  try {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined)
      queryParams.append("page", params.page.toString());
    if (params.size) queryParams.append("size", params.size.toString());

    const response = await api.get<ArchivedLogsResponse>(
      `/log/${params.projectId}/archive?${queryParams.toString()}`,
      {
        headers: {
          "Content-Type": "application/json",
        },
      }
    );
    return response.data;
  } catch (error: any) {
    const status = error.response?.status;
    let errorCode = "INTERNAL_ERROR";
    if (status === 400) errorCode = "INVALID_REQUEST";
    else if (status === 401) errorCode = "UNAUTHORIZED";
    else if (status === 404) errorCode = "PROJECT_NOT_FOUND";

    return rejectWithValue({
      success: false,
      data: {
        content: [],
        pageNumber: 0,
        totalPages: 0,
        totalElements: 0,
        pageSize: 10,
        first: true,
        last: true,
      },
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "아카이브된 로그 조회 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as ArchivedLogsResponse);
  }
});

const initialState: LogState = {
  isLoading: false,
  error: null,
  stats: null,
  projectId: null,
  errorStats: [],
  errorTimeline: [],
  errorTypeRatios: [],
  errorTrends: [],
  logs: [],
  traceLogs: [],
  pagination: null,
  logDetail: null,
  archiveResult: null,
  archivedLogs: null, // Add this line
};

const logSlice = createSlice({
  name: "log",
  initialState,
  reducers: {
    resetLogStats: (state) => {
      state.stats = null;
      state.error = null;
      state.projectId = null;
      state.errorStats = [];
      state.errorTimeline = [];
      state.errorTypeRatios = [];
      state.errorTrends = [];
      state.logs = [];
      state.traceLogs = []; // Reset traceLogs
      state.pagination = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchLogStats.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchLogStats.fulfilled, (state, action) => {
        state.isLoading = false;
        state.stats = action.payload.data;
        state.error = null;
      })
      // Update error handling pattern
      .addCase(fetchLogStats.rejected, (state, action) => {
        state.isLoading = false;
        const payloadError = (action.payload as LogStatsResponse)?.error;
        state.error = payloadError
          ? {
              code: payloadError.code as
                | "INVALID_REQUEST"
                | "UNAUTHORIZED"
                | "NOT_FOUND"
                | "INTERNAL_ERROR",
              message: payloadError.message,
            }
          : {
              code: "INTERNAL_ERROR",
              message: "알 수 없는 오류가 발생했습니다.",
            };
      })

      // Update all rejected cases with consistent type casting
      .addCase(fetchErrorStats.rejected, (state, action) => {
        state.isLoading = false;
        const payloadError = (action.payload as ErrorStatsResponse)?.error;
        state.error = payloadError
          ? {
              code: payloadError.code as
                | "INVALID_REQUEST"
                | "UNAUTHORIZED"
                | "NOT_FOUND"
                | "INTERNAL_ERROR",
              message: payloadError.message,
            }
          : {
              code: "INTERNAL_ERROR",
              message: "알 수 없는 오류가 발생했습니다.",
            };
      })
      .addCase(fetchErrorTimeline.fulfilled, (state, action) => {
        state.isLoading = false;
        state.errorTimeline = action.payload.data;
        state.error = null;
      })

      .addCase(fetchErrorTimeline.rejected, (state, action) => {
        state.isLoading = false;
        const payloadError = (action.payload as ErrorTimelineResponse)?.error;
        state.error = payloadError
          ? {
              code: payloadError.code as
                | "INVALID_REQUEST"
                | "UNAUTHORIZED"
                | "NOT_FOUND"
                | "INTERNAL_ERROR",
              message: payloadError.message,
            }
          : {
              code: "INTERNAL_ERROR",
              message: "알 수 없는 오류가 발생했습니다.",
            };
      })

      .addCase(fetchErrorTypeRatio.rejected, (state, action) => {
        state.isLoading = false;
        const payloadError = (action.payload as ErrorTypeRatioResponse)?.error;
        state.error = payloadError
          ? {
              code: payloadError.code as
                | "INVALID_REQUEST"
                | "UNAUTHORIZED"
                | "NOT_FOUND"
                | "INTERNAL_ERROR",
              message: payloadError.message,
            }
          : {
              code: "INTERNAL_ERROR",
              message: "알 수 없는 오류가 발생했습니다.",
            };
      })

      .addCase(fetchErrorTrend.rejected, (state, action) => {
        state.isLoading = false;
        const payloadError = (action.payload as ErrorTrendResponse)?.error;
        state.error = payloadError
          ? {
              code: payloadError.code as
                | "INVALID_REQUEST"
                | "UNAUTHORIZED"
                | "NOT_FOUND"
                | "INTERNAL_ERROR",
              message: payloadError.message,
            }
          : {
              code: "INTERNAL_ERROR",
              message: "알 수 없는 오류가 발생했습니다.",
            };
      })

      .addCase(searchLogs.fulfilled, (state, action) => {
        state.isLoading = false;
        state.logs = action.payload.data.content;
        state.pagination = action.payload.data;
        state.error = null;
      })

      .addCase(searchLogs.rejected, (state, action) => {
        state.isLoading = false;
        const payloadError = (action.payload as LogSearchResponse)?.error;
        state.error = payloadError
          ? {
              code: payloadError.code as
                | "INVALID_REQUEST"
                | "UNAUTHORIZED"
                | "NOT_FOUND"
                | "INTERNAL_ERROR",
              message: payloadError.message,
            }
          : {
              code: "INTERNAL_ERROR",
              message: "알 수 없는 오류가 발생했습니다.",
            };
      })
      .addCase(fetchLogDetail.fulfilled, (state, action) => {
        state.isLoading = false;
        state.logDetail = action.payload.data;
        state.error = null;
      })
      .addCase(fetchLogDetail.rejected, (state, action) => {
        state.isLoading = false;
        state.logDetail = null;
        const payloadError = (action.payload as LogDetailResponse)?.error;
        state.error = payloadError
          ? {
              code: payloadError.code as
                | "INVALID_REQUEST"
                | "UNAUTHORIZED"
                | "NOT_FOUND"
                | "INTERNAL_ERROR",
              message: payloadError.message,
            }
          : {
              code: "INTERNAL_ERROR",
              message: "알 수 없는 오류가 발생했습니다.",
            };
      })
      .addCase(archiveLog.pending, (state) => {
        state.isLoading = true;
        state.error = null;
        state.archiveResult = null;
      })
      .addCase(archiveLog.fulfilled, (state, action) => {
        state.isLoading = false;
        state.archiveResult = action.payload.data;
        state.error = null;
      })
      .addCase(archiveLog.rejected, (state, action) => {
        state.isLoading = false;
        state.archiveResult = null;

        const payload = action.payload as ArchiveLogResponse | undefined;

        if (payload?.error?.code && typeof payload.error.message === "string") {
          // 안전하게 에러 코드 제한
          const knownCodes = [
            "INVALID_REQUEST",
            "UNAUTHORIZED",
            "LOG_NOT_FOUND",
            "INTERNAL_ERROR",
          ] as const;
          const code = knownCodes.includes(payload.error.code as any)
            ? (payload.error.code as (typeof knownCodes)[number])
            : "INTERNAL_ERROR";

          state.error = {
            code: "INTERNAL_ERROR",
            message: payload.error.message,
          };
        } else {
          state.error = {
            code: "INTERNAL_ERROR",
            message: "알 수 없는 오류가 발생했습니다.",
          };
        }
      })
      .addCase(fetchLogs.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchLogs.fulfilled, (state, action) => {
        state.isLoading = false;
        state.logs = action.payload.data.content;
        state.pagination = {
          pageNumber: action.payload.data.pageNumber,
          totalPages: action.payload.data.totalPages,
          totalElements: action.payload.data.totalElements,
          pageSize: action.payload.data.pageSize,
          first: action.payload.data.first,
          last: action.payload.data.last,
        };
        state.error = null;
      })
      .addCase(fetchLogs.rejected, (state, action) => {
        state.isLoading = false;
        state.logs = [];
        state.pagination = null;

        const payloadError = (action.payload as LogListResponse)?.error;
        state.error = payloadError
          ? {
              code: payloadError.code as
                | "INVALID_REQUEST"
                | "UNAUTHORIZED"
                | "NOT_FOUND"
                | "INTERNAL_ERROR",
              message: payloadError.message,
            }
          : {
              code: "INTERNAL_ERROR",
              message: "알 수 없는 오류가 발생했습니다.",
            };
      })
      .addCase(fetchTraceLog.pending, (state) => {
        state.isLoading = true;
        state.error = null;
        state.traceLogs = [];
      })
      .addCase(fetchTraceLog.fulfilled, (state, action) => {
        state.isLoading = false;
        state.traceLogs = action.payload.data;
        state.error = null;
      })
      .addCase(fetchTraceLog.rejected, (state, action) => {
        state.isLoading = false;
        state.traceLogs = [];

        const payloadError = (action.payload as TraceLogResponse)?.error;
        state.error = payloadError
          ? {
              code: payloadError.code as
                | "INVALID_REQUEST"
                | "UNAUTHORIZED"
                | "NOT_FOUND"
                | "INTERNAL_ERROR",
              message: payloadError.message,
            }
          : {
              code: "INTERNAL_ERROR",
              message: "Trace 로그 조회 중 오류가 발생했습니다.",
            };
      })
      .addCase(fetchArchivedLogs.pending, (state) => {
        state.isLoading = true;
        state.error = null;
        state.archivedLogs = null;
      })
      .addCase(fetchArchivedLogs.fulfilled, (state, action) => {
        state.isLoading = false;
        state.archivedLogs = action.payload.data; // ✅ 전체 객체 저장
        state.pagination = {
          pageNumber: action.payload.data.pageNumber,
          totalPages: action.payload.data.totalPages,
          totalElements: action.payload.data.totalElements,
          pageSize: action.payload.data.pageSize,
          first: action.payload.data.first,
          last: action.payload.data.last,
        };
        state.error = null;
      })
      .addCase(fetchArchivedLogs.rejected, (state, action) => {
        state.isLoading = false;
        state.archivedLogs = null;
        state.pagination = null;

        const payloadError = (action.payload as ArchivedLogsResponse)?.error;
        state.error = payloadError
          ? {
              code: payloadError.code as
                | "INVALID_REQUEST"
                | "UNAUTHORIZED"
                | "NOT_FOUND"
                | "INTERNAL_ERROR",
              message: payloadError.message,
            }
          : {
              code: "INTERNAL_ERROR",
              message: "아카이브된 로그 조회 중 오류가 발생했습니다.",
            };
      });
  },
});

export const { resetLogStats } = logSlice.actions;
export default logSlice.reducer;
