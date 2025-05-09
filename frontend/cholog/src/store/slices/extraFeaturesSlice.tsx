import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../../api/axios';
import { 
  UpdateJiraTokenRequest, 
  UpdateJiraTokenResponse, 
  GetJiraTokenResponse,
  ExtraFeaturesState 
} from '../../types/extraFeatures.types';

/**
 * [#EXTRAFEATURES-1]
 * ============================================
 * [PUT] /project/:projectId/jira
 * JIRA 토큰을 등록/수정합니다.
 * --------------------------------------------
 * @param params - projectId: 프로젝트 ID, token: JIRA 토큰
 * @returns UpdateJiraTokenResponse
 * - 토큰이 등록/수정된 프로젝트 ID 반환
 * ============================================
 */
export const updateJiraToken = createAsyncThunk<UpdateJiraTokenResponse, UpdateJiraTokenRequest>(
  'extraFeatures/updateJiraToken',
  async ({ projectId, token }, { rejectWithValue }) => {
    try {
      const response = await api.put<UpdateJiraTokenResponse>(
        `/project/${projectId}/jira`,
        { token },
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = 'INTERNAL_ERROR';
      
      if (status === 400) errorCode = 'INVALID_REQUEST';
      else if (status === 401) errorCode = 'UNAUTHORIZED';

      return rejectWithValue({
        success: false,
        data: { projectId },
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || 'JIRA 토큰 등록/수정 중 오류가 발생했습니다.',
        },
        timestamp: new Date().toISOString(),
      } as UpdateJiraTokenResponse);
    }
  }
);

/**
 * ============================================
 * [#EXTRAFEATURES-2]
 * [GET] /api/jira/{projectId}/token
 * JIRA 토큰을 조회합니다.
 * --------------------------------------------
 * @param projectId - JIRA 토큰을 조회할 프로젝트 ID
 * @returns GetJiraTokenResponse
 * - 프로젝트 ID와 JIRA 토큰 정보 반환
 * ============================================
 */
export const getJiraToken = createAsyncThunk<GetJiraTokenResponse, number>(
  'extraFeatures/getJiraToken',
  async (projectId, { rejectWithValue }) => {
    try {
      const response = await api.get<GetJiraTokenResponse>(
        `/api/jira/${projectId}/token`,
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = 'INTERNAL_ERROR';
      
      if (status === 400) errorCode = 'INVALID_REQUEST';
      else if (status === 401) errorCode = 'UNAUTHORIZED';

      return rejectWithValue({
        success: false,
        data: { projectId, token: '' },
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || 'JIRA 토큰 조회 중 오류가 발생했습니다.',
        },
        timestamp: new Date().toISOString(),
      } as GetJiraTokenResponse);
    }
  }
);

const initialState: ExtraFeaturesState = {
  isLoading: false,
  error: null,
  jiraToken: null,
};

const extraFeaturesSlice = createSlice({
  name: 'extraFeatures',
  initialState,
  reducers: {
    resetExtraFeatures: (state) => {
      state.error = null;
      state.jiraToken = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(updateJiraToken.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(updateJiraToken.fulfilled, (state) => {
        state.isLoading = false;
        state.error = null;
      })
      .addCase(updateJiraToken.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as UpdateJiraTokenResponse)?.error ?? {
          code: 'INTERNAL_ERROR',
          message: '알 수 없는 오류가 발생했습니다.',
        };
      })
      .addCase(getJiraToken.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(getJiraToken.fulfilled, (state, action) => {
        state.isLoading = false;
        state.error = null;
        state.jiraToken = action.payload.data;
      })
      .addCase(getJiraToken.rejected, (state, action) => {
        state.isLoading = false;
        state.jiraToken = null;
        state.error = (action.payload as GetJiraTokenResponse)?.error ?? {
          code: 'INTERNAL_ERROR',
          message: '알 수 없는 오류가 발생했습니다.',
        };
      });
  },
});

export const { resetExtraFeatures } = extraFeaturesSlice.actions;
export default extraFeaturesSlice.reducer;

