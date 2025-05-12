/**
 * @description 리포트 관련 상태 관리 슬라이스
 * @author Cholog FE Team
 */

import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from 'axios';
import { 
  Report, 
  ReportListData, 
  ReportListResponse,
  ReportSuccessResponse
} from '../../types/report.types';

/**
 * @description 리포트 상태 인터페이스
 * @property {Report | null} currentReport - 현재 조회중인 리포트 정보
 * @property {ReportListData | null} reportList - 리포트 목록 데이터
 * @property {boolean} isLoading - 로딩 상태
 * @property {string | null} error - 에러 메시지
 */
interface ReportState {
  currentReport: Report | null;
  reportList: ReportListData | null;
  isLoading: boolean;
  error: string | null;
}

/**
 * @description 리포트 상태 초기값
 */
const initialState: ReportState = {
  currentReport: null,
  reportList: null,
  isLoading: false,
  error: null,
};

/**
 * ============================================
 * [#REPORT-1]
 * [GET] /api/report/:projectId/:reportId
 * 특정 리포트의 상세 정보를 조회합니다.
 * --------------------------------------------
 * @param projectId - 프로젝트 식별자
 * @param reportId - 리포트 식별자
 * @returns Report - 리포트 상세 정보
 * ============================================
 */
export const fetchReportDetail = createAsyncThunk<
  Report,
  { projectId: number; reportId: string }
>('report/fetchDetail', async ({ projectId, reportId }) => {
  const response = await axios.get<ReportSuccessResponse>(
    `/api/report/${projectId}/${reportId}`
  );
  if (!response.data.success) {
    throw new Error((response.data as any).error.message);
  }
  return response.data.data;
});

/**
 * ============================================
 * [#REPORT-2]
 * [GET] /api/report/:projectId
 * 프로젝트의 전체 리포트 목록을 조회합니다.
 * --------------------------------------------
 * @param projectId - 프로젝트 식별자
 * @returns ReportListData - 리포트 목록 및 페이지네이션 정보
 * ============================================
 */
export const fetchReportList = createAsyncThunk<
  ReportListData,
  { projectId: number }
>('report/fetchList', async ({ projectId }) => {
  const response = await axios.get<ReportListResponse>(
    `/api/report/${projectId}`
  );
  if (!response.data.success) {
    throw new Error((response.data as any).error.message);
  }
  return response.data.data;
});

/**
 * @description 리포트 관련 상태 관리 슬라이스
 */
const reportSlice = createSlice({
  name: 'report',
  initialState,
  reducers: {
    /**
     * 현재 조회중인 리포트 정보를 초기화합니다.
     */
    clearCurrentReport: (state) => {
      state.currentReport = null;
    },
    /**
     * 리포트 목록 데이터를 초기화합니다.
     */
    clearReportList: (state) => {
      state.reportList = null;
    },
  },
  extraReducers: (builder) => {
    // 리포트 상세 조회
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
        state.error = action.error.message || '리포트 조회 중 오류가 발생했습니다.';
      })
    
    // 리포트 목록 조회
      .addCase(fetchReportList.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchReportList.fulfilled, (state, action) => {
        state.isLoading = false;
        state.reportList = action.payload;
      })
      .addCase(fetchReportList.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.error.message || '리포트 목록 조회 중 오류가 발생했습니다.';
      });
  },
});

export const { clearCurrentReport, clearReportList } = reportSlice.actions;
export default reportSlice.reducer;