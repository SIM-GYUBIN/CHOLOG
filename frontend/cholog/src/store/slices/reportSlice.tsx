import { createSlice, createAsyncThunk, PayloadAction } from "@reduxjs/toolkit";
import api from "../../api/axios";
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

// --- PDF 다운로드 Thunk를 위한 타입 정의 ---
/**
 * @description PDF 다운로드 요청 파라미터
 * @property {number | string} projectId - 프로젝트 ID
 * @property {string} htmlContent - PDF로 변환할 HTML 전체 내용
 * @property {string} [startDate] - 리포트 기간 시작일 (주로 파일명 생성에 사용)
 * @property {string} [endDate] - 리포트 기간 종료일 (주로 파일명 생성에 사용)
 */
export interface DownloadPdfParams {
  projectId: number | string;
  htmlContent: string;
  startDate?: string;
  endDate?: string;
}

/**
 * @description PDF 다운로드 성공 시 Thunk 반환 값 타입
 */
interface PdfDownloadSuccessPayload {
  message: string;
  filename: string;
}

/**
 * [#REPORT-2]
 * @description 리포트 상태 인터페이스
 * @property {Report | null} currentReport - 현재 조회된 리포트
 * @property {boolean} isLoading - 리포트 데이터 로딩 상태
 * @property {string | null} error - 리포트 데이터 로딩 에러 메시지
 * @property {string | null} reportGeneratedStartDate - 현재 리포트 생성 시 사용된 시작일
 * @property {string | null} reportGeneratedEndDate - 현재 리포트 생성 시 사용된 종료일
 * @property {boolean} isGeneratingPdf - PDF 생성 중 로딩 상태
 */
interface ReportState {
  currentReport: Report | null;
  isLoading: boolean;
  error: string | null;
  reportGeneratedStartDate: string | null; // PDF 파일명 등에 활용하기 위해 추가
  reportGeneratedEndDate: string | null; // PDF 파일명 등에 활용하기 위해 추가
  isGeneratingPdf: boolean; // PDF 생성 관련 상태 추가
  pdfError: string | null; // PDF 생성 관련 상태 추가
}

/**
 * @description 초기 상태 정의
 */
const initialState: ReportState = {
  currentReport: null,
  isLoading: false,
  error: null,
  reportGeneratedStartDate: null,
  reportGeneratedEndDate: null,
  isGeneratingPdf: false,
  pdfError: null,
};

/**
 * [#REPORT-3]
 * @description 리포트 생성 및 조회 API 호출
 * @route POST /api/report/:projectId (기존 API 경로 예시)
 */
export const fetchReportDetail = createAsyncThunk<
  Report, // 성공 시 반환 타입
  FetchReportDetailParams, // Thunk 파라미터 타입
  { rejectValue: string } // 실패 시 반환 타입
>(
  "report/fetchDetail",
  async ({ projectId, startDate, endDate }, { rejectWithValue }) => {
    try {
      const response = await api.post<ReportSuccessResponse>(
        `/report/${projectId}`,
        { startDate, endDate },
        {
          headers: {
            "Content-Type": "application/json",
          },
        }
      );
      return response.data.data;
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        error.message ||
        "리포트 데이터 요청 실패";
      return rejectWithValue(message);
    }
  }
);

/**
 * @description PDF 리포트 생성 및 다운로드 API 호출
 * @route POST /api/reports/:projectId/download-pdf (Spring Boot PDF API 경로 예시)
 */
export const downloadPdfReport = createAsyncThunk<
  PdfDownloadSuccessPayload,
  DownloadPdfParams,
  { rejectValue: string }
>("report/downloadPdf", async (params, { rejectWithValue }) => {
  const { projectId, htmlContent, startDate, endDate } = params;

  try {
    const requestPayload = {
      htmlContent,
      startDate, // 서버에서 파일명 등에 사용 가능
      endDate, // 서버에서 파일명 등에 사용 가능
    };

    const response = await api.post(
      `/report/${projectId}/pdf`,
      requestPayload,
      {
        headers: {
          "Content-Type": "application/json",
        },
        responseType: "blob", // 서버가 PDF 파일을 반환하므로 blob으로 응답을 받습니다.
      }
    );

    // 파일 다운로드 로직
    const pdfBlob = new Blob([response.data], { type: "application/pdf" });
    const downloadUrl = window.URL.createObjectURL(pdfBlob);
    const link = document.createElement("a");
    link.href = downloadUrl;

    // 클라이언트 측 파일명 생성 (서버의 Content-Disposition 헤더가 우선될 수 있음)
    const projectIdentifier = `ChoLog`;
    let periodSuffix = "Period";
    if (startDate && endDate) {
      periodSuffix = `${startDate}_to_${endDate}`;
    } else if (startDate) {
      periodSuffix = startDate;
    }
    const filename = `${projectIdentifier}_Report_${periodSuffix}.pdf`
      .replace(/:/g, "-")
      .replace(/\s+/g, "_");
    link.download = filename;

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);

    return { message: "PDF가 성공적으로 다운로드되었습니다.", filename };
  } catch (error: any) {
    let errorMessage =
      "PDF 생성 또는 다운로드 중 알 수 없는 오류가 발생했습니다.";
    if (error.response && error.response.data) {
      if (error.response.data instanceof Blob) {
        try {
          errorMessage = await error.response.data.text();
        } catch (e) {
          /* Blob 텍스트 변환 실패 */
        }
      } else if (typeof error.response.data === "string") {
        errorMessage = error.response.data;
      } else if (error.response.data.message) {
        errorMessage = error.response.data.message;
      }
    } else if (error.message) {
      errorMessage = error.message;
    }
    return rejectWithValue(errorMessage);
  }
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
      state.reportGeneratedStartDate = null;
      state.reportGeneratedEndDate = null;
      state.error = null;
    },
    /**
     * PDF 생성 오류 메시지 초기화
     */
    clearPdfError: (state) => {
      state.pdfError = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchReportDetail.pending, (state) => {
        state.isLoading = true;
        state.error = null;
        state.currentReport = null; // 새 리포트 로딩 시 이전 데이터 초기화
        state.reportGeneratedStartDate = null;
        state.reportGeneratedEndDate = null;
      })
      // PayloadAction의 제네릭에 meta 정보를 추가하여 arg (Thunk 파라미터)에 접근
      .addCase(
        fetchReportDetail.fulfilled,
        (
          state,
          action: PayloadAction<
            Report,
            string,
            { arg: FetchReportDetailParams }
          >
        ) => {
          state.isLoading = false;
          state.currentReport = action.payload;
          // 리포트 생성에 사용된 날짜를 상태에 저장 (PDF 파일명 등에 활용)
          state.reportGeneratedStartDate = action.meta.arg.startDate;
          state.reportGeneratedEndDate = action.meta.arg.endDate;
        }
      )
      .addCase(fetchReportDetail.rejected, (state, action) => {
        state.isLoading = false;
        // action.payload는 rejectWithValue에서 전달된 값, action.error.message는 일반 오류 메시지
        state.error =
          action.payload || action.error.message || "리포트 요청 실패";
        alert("오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
      })

      // downloadPdfReport Thunk
      .addCase(downloadPdfReport.pending, (state) => {
        state.isGeneratingPdf = true;
        state.pdfError = null;
      })
      .addCase(downloadPdfReport.fulfilled, (state, action) => {
        state.isGeneratingPdf = false;
        // 필요시 여기에 성공 알림(toast 등) 로직 추가
      })
      .addCase(downloadPdfReport.rejected, (state, action) => {
        state.isGeneratingPdf = false;
        state.pdfError =
          action.payload || action.error.message || "PDF 생성/다운로드 실패";
        console.error(
          "PDF 다운로드 Thunk 오류:",
          action.error,
          "페이로드:",
          action.payload
        );
      });
  },
});

export const { clearCurrentReport, clearPdfError } = reportSlice.actions;
export default reportSlice.reducer;
