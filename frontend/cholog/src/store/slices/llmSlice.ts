import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import api from "../../api/axios";

// 타입 정의
interface LlmAnalysisResponse {
  success: boolean;
  data: {
    analysisResult: string;
    modelUsed: string;
  };
  error?: {
    code: string;
    message: string;
  };
  timestamp: string;
}

interface LlmAnalysisRequest {
  projectId: string;
  logId: string;
}

interface LlmState {
  analysis: {
    result: string;
    isLoading: boolean;
    error: string | null;
  };
}

// LLM 분석 액션 생성
export const analyzeLLM = createAsyncThunk<LlmAnalysisResponse, LlmAnalysisRequest>(
  "llm/analyze",
  async ({ projectId, logId }, { rejectWithValue }) => {
    try {
      const response = await api.post<LlmAnalysisResponse>(
        `/log/${projectId}/analysis`,
        { logId },
        {
          headers: {
            "Content-Type": "application/json",
          },
          withCredentials: true
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
          analysisResult: "",
          modelUsed: "",
        },
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || "LLM 분석 중 오류가 발생했습니다.",
        },
        timestamp: new Date().toISOString(),
      } as LlmAnalysisResponse);
    }
  }
);

// 초기 상태
const initialState: LlmState = {
  analysis: {
    result: "",
    isLoading: false,
    error: null,
  },
};

// 슬라이스 생성
const llmSlice = createSlice({
  name: "llm",
  initialState,
  reducers: {
    resetAnalysis: (state) => {
      state.analysis = initialState.analysis;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(analyzeLLM.pending, (state) => {
        state.analysis.isLoading = true;
        state.analysis.error = null;
      })
      .addCase(analyzeLLM.fulfilled, (state, action) => {
        state.analysis.isLoading = false;
        state.analysis.result = action.payload.data.analysisResult;
        state.analysis.error = null;
      })
      .addCase(analyzeLLM.rejected, (state, action: any) => {
        state.analysis.isLoading = false;
        state.analysis.error = action.payload?.error?.message || "분석 중 오류가 발생했습니다.";
      });
  },
});

export const { resetAnalysis } = llmSlice.actions;
export default llmSlice.reducer;