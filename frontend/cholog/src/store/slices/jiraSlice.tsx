/**
 * @description Jira 관련 상태 관리 슬라이스
 * @author Cholog FE Team
 */

import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from "../../api/axios";

// Jira 관련 인터페이스 정의
interface JiraUserSettings {
  userName?: string;
  jiraToken?: string;
  isEnabled?: boolean;
}

interface JiraProjectSettings {
  id?: number;
  projectKey?: string;
  isEnabled?: boolean;
  instanceUrl?: string; // 추가: Jira 인스턴스 URL
}

interface JiraIssueInfo {
  userNames: any[];
  projectId: number;
  issueTypes?: Array<{
    id: string;
    name: string;
  }>;
  priorities?: Array<{
    id: string;
    name: string;
  }>;
}

interface JiraIssueCreateRequest {
  projectId: number;
  summary: string;
  description?: string;
  issueType: string;
  reporterName: string;
  assigneeName?: string;
}

// 응답 인터페이스
interface BaseResponse {
  success: boolean;
  timestamp: string;
  error?: {
    code: string;
    message: string;
  };
}

// API 명세서에 맞게 수정된 JiraUserResponse 인터페이스
interface JiraUserGetResponse extends BaseResponse {
  data: {
    exists: boolean;
    userName?: string;
    jiraToken?: string;
  };
}

interface JiraUserCreateUpdateResponse extends BaseResponse {
  data?: {}; // 성공 시 빈 객체 반환
}

// API 명세서에 맞게 수정된 JiraProjectResponse 인터페이스
interface JiraProjectResponse extends BaseResponse {
  data: {
    exists: boolean;
    instanceUrl?: string;
    projectKey?: string;
  };
}

interface JiraIssueInfoResponse extends BaseResponse {
  data: JiraIssueInfo;
}

interface JiraIssueCreateResponse extends BaseResponse {
  data: {
    issueKey: string;
    issueUrl: string;
  };
}

// Jira 상태 인터페이스
interface JiraState {
  userSettings: JiraUserSettings | null;
  projectSettings: JiraProjectSettings | null;
  issueInfo: JiraIssueInfo | null;
  createdIssue: {
    issueKey: string;
    issueUrl: string;
  } | null;
  isLoading: boolean;
  error: string | null;
}

// 초기 상태
const initialState: JiraState = {
  userSettings: null,
  projectSettings: null,
  issueInfo: null,
  createdIssue: null,
  isLoading: false,
  error: null
};

/**
 * ============================================
 * [GET] /jira/user
 * JIRA 계정 설정 조회
 * ============================================
 */
export const fetchJiraUserSettings = createAsyncThunk<JiraUserGetResponse>(
  'jira/fetchUserSettings',
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.get<JiraUserGetResponse>('/jira/user', {
        headers: {
          'Content-Type': 'application/json',
        }
      });
      return response.data;
    } catch (error: any) {
      return rejectWithValue({
        success: false,
        data: { exists: false },
        error: {
          code: error.response?.status === 401 ? 'UNAUTHORIZED' : 'INTERNAL_ERROR',
          message: error.response?.data?.error?.message || 'JIRA 계정 설정 조회 중 오류가 발생했습니다.'
        },
        timestamp: new Date().toISOString()
      } as JiraUserGetResponse);
    }
  }
);

/**
 * ============================================
 * [PUT] /jira/user
 * JIRA 계정 설정 수정
 * ============================================
 */
export const updateJiraUserSettings = createAsyncThunk<JiraUserCreateUpdateResponse, { userName: string, jiraToken: string }>(
  'jira/updateUserSettings',
  async (userSettings, { rejectWithValue }) => {
    try {
      const response = await api.put<JiraUserCreateUpdateResponse>('/jira/user', userSettings, {
        headers: {
          'Content-Type': 'application/json',
        }
      });
      return response.data;
    } catch (error: any) {
      return rejectWithValue({
        success: false,
        data: {},
        error: {
          code: error.response?.status === 401 ? 'UNAUTHORIZED' : 
                error.response?.status === 400 ? 'INVALID_REQUEST' : 'INTERNAL_ERROR',
          message: error.response?.data?.error?.message || 'JIRA 계정 설정 수정 중 오류가 발생했습니다.'
        },
        timestamp: new Date().toISOString()
      } as JiraUserCreateUpdateResponse);
    }
  }
);

/**
 * ============================================
 * [POST] /jira/user
 * JIRA 계정 설정 등록
 * ============================================
 */
export const createJiraUserSettings = createAsyncThunk<JiraUserCreateUpdateResponse, { userName: string, jiraToken: string }>(
  'jira/createUserSettings',
  async (userSettings, { rejectWithValue }) => {
    try {
      const response = await api.post<JiraUserCreateUpdateResponse>('/jira/user', userSettings, {
        headers: {
          'Content-Type': 'application/json',
        }
      });
      return response.data;
    } catch (error: any) {
      return rejectWithValue({
        success: false,
        data: {},
        error: {
          code: error.response?.status === 401 ? 'UNAUTHORIZED' : 
                error.response?.status === 400 ? 'INVALID_REQUEST' : 'INTERNAL_ERROR',
          message: error.response?.data?.error?.message || 'JIRA 계정 설정 등록 중 오류가 발생했습니다.'
        },
        timestamp: new Date().toISOString()
      } as JiraUserCreateUpdateResponse);
    }
  }
);

/**
 * ============================================
 * [GET] /api/jira/project/{projectId}
 * JIRA 프로젝트 설정 조회
 * ============================================
 */
export const fetchJiraProjectSettings = createAsyncThunk<JiraProjectResponse, number>(
  'jira/fetchProjectSettings',
  async (projectId, { rejectWithValue }) => {
    try {
      const response = await api.get<JiraProjectResponse>(`/jira/project/${projectId}`, {
        headers: {
          'Content-Type': 'application/json',
        }
      });
      return response.data;
    } catch (error: any) {
      return rejectWithValue({
        success: false,
        data: { exists: false },
        error: {
          code: error.response?.status === 401 ? 'UNAUTHORIZED' : 'INTERNAL_ERROR',
          message: error.response?.data?.error?.message || 'JIRA 프로젝트 설정 조회 중 오류가 발생했습니다.'
        },
        timestamp: new Date().toISOString()
      } as JiraProjectResponse);
    }
  }
);

/**
 * ============================================
 * [PUT] /api/jira/project/{projectId}
 * JIRA 프로젝트 설정 수정
 * ============================================
 */
export const updateJiraProjectSettings = createAsyncThunk<
  JiraProjectResponse, 
  { projectId: number; settings: JiraProjectSettings }
>(
  'jira/updateProjectSettings',
  async ({ projectId, settings }, { rejectWithValue }) => {
    try {
      const response = await api.put<JiraProjectResponse>(
        `/jira/project/${projectId}`, 
        settings, 
        {
          headers: {
            'Content-Type': 'application/json',
          }
        }
      );
      return response.data;
    } catch (error: any) {
      return rejectWithValue({
        success: false,
        data: { exists: false },
        error: {
          code: error.response?.status === 401 ? 'UNAUTHORIZED' : 'INTERNAL_ERROR',
          message: error.response?.data?.error?.message || 'JIRA 프로젝트 설정 수정 중 오류가 발생했습니다.'
        },
        timestamp: new Date().toISOString()
      } as JiraProjectResponse);
    }
  }
);

/**
 * ============================================
 * [POST] /api/jira/project/{projectId}
 * JIRA 프로젝트 설정 등록
 * ============================================
 */
export const createJiraProjectSettings = createAsyncThunk<
  JiraProjectResponse, 
  { projectId: number; settings: JiraProjectSettings }
>(
  'jira/createProjectSettings',
  async ({ projectId, settings }, { rejectWithValue }) => {
    try {
      const response = await api.post<JiraProjectResponse>(
        `/jira/project/${projectId}`, 
        settings, 
        {
          headers: {
            'Content-Type': 'application/json',
          }
        }
      );
      return response.data;
    } catch (error: any) {
      return rejectWithValue({
        success: false,
        data: { exists: false },
        error: {
          code: error.response?.status === 401 ? 'UNAUTHORIZED' : 'INTERNAL_ERROR',
          message: error.response?.data?.error?.message || 'JIRA 프로젝트 설정 등록 중 오류가 발생했습니다.'
        },
        timestamp: new Date().toISOString()
      } as JiraProjectResponse);
    }
  }
);

/**
 * ============================================
 * [GET] /api/jira/issue/{projectId}
 * JIRA 이슈 생성을 위한 정보 조회
 * ============================================
 */
export const fetchJiraIssueInfo = createAsyncThunk<JiraIssueInfoResponse, number>(
  'jira/fetchIssueInfo',
  async (projectId, { rejectWithValue }) => {
    try {
      const response = await api.get<JiraIssueInfoResponse>(`/jira/issue/${projectId}`, {
        headers: {
          'Content-Type': 'application/json',
        }
      });
      return response.data;
    } catch (error: any) {
      return rejectWithValue({
        success: false,
        data: { projectId },
        error: {
          code: error.response?.status === 401 ? 'UNAUTHORIZED' : 'INTERNAL_ERROR',
          message: error.response?.data?.error?.message || 'JIRA 이슈 정보 조회 중 오류가 발생했습니다.'
        },
        timestamp: new Date().toISOString()
      } as JiraIssueInfoResponse);
    }
  }
);

/**
 * ============================================
 * [POST] /api/jira/issue/{projectId}
 * JIRA 이슈 생성
 * ============================================
 */
export const createJiraIssue = createAsyncThunk<JiraIssueCreateResponse, JiraIssueCreateRequest>(
  'jira/createIssue',
  async (issueData, { rejectWithValue }) => {
    try {
      const response = await api.post<JiraIssueCreateResponse>(
        `/jira/issue/${issueData.projectId}`, 
        {
          summary: issueData.summary,
          description: issueData.description,
          issueType: issueData.issueType,
          reporterName: issueData.reporterName,
          assigneeName: issueData.assigneeName
        }, 
        {
          headers: {
            'Content-Type': 'application/json',
          }
        }
      );
      return response.data;
    } catch (error: any) {
      return rejectWithValue({
        success: false,
        data: { issueKey: '', issueUrl: '' },
        error: {
          code: error.response?.status === 401 ? 'UNAUTHORIZED' : 'INTERNAL_ERROR',
          message: error.response?.data?.error?.message || 'JIRA 이슈 생성 중 오류가 발생했습니다.'
        },
        timestamp: new Date().toISOString()
      } as JiraIssueCreateResponse);
    }
  }
);

// Jira 슬라이스 생성
const jiraSlice = createSlice({
  name: 'jira',
  initialState,
  reducers: {
    resetJiraState: (state) => {
      state.userSettings = null;
      state.projectSettings = null;
      state.issueInfo = null;
      state.createdIssue = null;
      state.error = null;
    },
    resetCreatedIssue: (state) => {
      state.createdIssue = null;
    }
  },
  extraReducers: (builder) => {
    // 사용자 설정 조회
    builder
      .addCase(fetchJiraUserSettings.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchJiraUserSettings.fulfilled, (state, action) => {
        state.isLoading = false;
        if (action.payload.success && action.payload.data.exists) {
          state.userSettings = {
            userName: action.payload.data.userName,
            jiraToken: action.payload.data.jiraToken
          };
        } else {
          state.userSettings = null;
        }
      })
      .addCase(fetchJiraUserSettings.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as JiraUserGetResponse)?.error?.message || '알 수 없는 오류가 발생했습니다.';
      })
      
    // 사용자 설정 수정
      .addCase(updateJiraUserSettings.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(updateJiraUserSettings.fulfilled, (state, action) => {
        state.isLoading = false;
        // 성공 시 상태 업데이트 (API에서는 별도 데이터를 반환하지 않음)
        if (action.payload.success) {
          // 기존 상태 유지 또는 필요한 경우 추가 작업
        }
      })
      .addCase(updateJiraUserSettings.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as JiraUserCreateUpdateResponse)?.error?.message || '알 수 없는 오류가 발생했습니다.';
      })
      
    // 사용자 설정 등록
      .addCase(createJiraUserSettings.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(createJiraUserSettings.fulfilled, (state, action) => {
        state.isLoading = false;
        // 성공 시 상태 업데이트 (API에서는 별도 데이터를 반환하지 않음)
        if (action.payload.success) {
          // 기존 상태 유지 또는 필요한 경우 추가 작업
        }
      })
      .addCase(createJiraUserSettings.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as JiraUserCreateUpdateResponse)?.error?.message || '알 수 없는 오류가 발생했습니다.';
      })
      
    // 프로젝트 설정 조회
      .addCase(fetchJiraProjectSettings.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchJiraProjectSettings.fulfilled, (state, action) => {
        state.isLoading = false;
        if (action.payload.success && action.payload.data.exists) {
          state.projectSettings = {
            projectKey: action.payload.data.projectKey || '',
            instanceUrl: action.payload.data.instanceUrl || '',
          };
        } else {
          state.projectSettings = null;
        }
      })
      .addCase(fetchJiraProjectSettings.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as JiraProjectResponse)?.error?.message || '알 수 없는 오류가 발생했습니다.';
      })
      
    // 프로젝트 설정 수정
      .addCase(updateJiraProjectSettings.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(updateJiraProjectSettings.fulfilled, (state, action) => {
        state.isLoading = false;
        // 성공 시 상태 유지 (API에서는 빈 객체를 반환)
      })
      .addCase(updateJiraProjectSettings.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as JiraProjectResponse)?.error?.message || '알 수 없는 오류가 발생했습니다.';
      })
      
    // 프로젝트 설정 등록
      .addCase(createJiraProjectSettings.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(createJiraProjectSettings.fulfilled, (state, action) => {
        state.isLoading = false;
        // 성공 시 상태 유지 (API에서는 빈 객체를 반환)
      })
      .addCase(createJiraProjectSettings.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as JiraProjectResponse)?.error?.message || '알 수 없는 오류가 발생했습니다.';
      })
      
    // 이슈 정보 조회
      .addCase(fetchJiraIssueInfo.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchJiraIssueInfo.fulfilled, (state, action) => {
        state.isLoading = false;
        state.issueInfo = action.payload.data;
      })
      .addCase(fetchJiraIssueInfo.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as JiraIssueInfoResponse)?.error?.message || '알 수 없는 오류가 발생했습니다.';
      })
      
    // 이슈 생성
      .addCase(createJiraIssue.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(createJiraIssue.fulfilled, (state, action) => {
        state.isLoading = false;
        state.createdIssue = action.payload.data;
      })
      .addCase(createJiraIssue.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as JiraIssueCreateResponse)?.error?.message || '알 수 없는 오류가 발생했습니다.';
      });
  }
});

export const { resetJiraState, resetCreatedIssue } = jiraSlice.actions;
export default jiraSlice.reducer;