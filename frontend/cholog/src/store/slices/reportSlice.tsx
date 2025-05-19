import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import axios from "axios";
import { Report, ReportSuccessResponse } from "../../types/report.types";

/**
 * [#REPORT-1]
 * @description 리포트 요청 파라미터
 * @property {number} projectId - 프로젝트 ID
 * @property {string} startDate - 조회 시작일 (YYYY-MM-DD)
 * @property {string} endDate - 조회 종료일 (YYYY-MM-DD)
 */
interface FetchReportDetailParams {
  projectId: number;
  startDate: string;
  endDate: string;
}

/**
 * [#REPORT-2]
 * @description 리포트 상태 인터페이스
 * @property {Report | null} currentReport - 현재 조회된 리포트
 * @property {boolean} isLoading - 로딩 상태
 * @property {string | null} error - 에러 메시지
 */
interface ReportState {
  currentReport: Report | null;
  isLoading: boolean;
  error: string | null;
}

/**
 * @description 초기 상태 정의
 */
const initialState: ReportState = {
  currentReport: null,
  isLoading: false,
  error: null,
};

/**
 * [#REPORT-3]
 * @description 리포트 생성 및 조회 API 호출
 * @route POST /api/report/:projectId
 */
export const fetchReportDetail = createAsyncThunk<
  Report,
  FetchReportDetailParams
>("report/fetchDetail", async ({ projectId, startDate, endDate }) => {
  const response = await axios.post<ReportSuccessResponse>(
    `/api/report/${projectId}`,
    { startDate, endDate },
    {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    }
  );

  return response.data.data;
});

/**
 * @description 리포트 슬라이스
 */
const reportSlice = createSlice({
  name: "report",
  initialState,
  reducers: {
    /**
     * 현재 리포트 정보 초기화
     */
    clearCurrentReport: (state) => {
      state.currentReport = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchReportDetail.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchReportDetail.fulfilled, (state, action) => {
        state.isLoading = false;
        state.currentReport = action.payload;
      })
      .addCase(fetchReportDetail.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.error.message || "리포트 요청 실패";
      });
  },
});

export const { clearCurrentReport } = reportSlice.actions;
export default reportSlice.reducer;
