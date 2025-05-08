import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../../api/axios';
import {
  ProjectListResponse,
  ProjectState,
  CreateProjectRequest,
  CreateProjectResponse,
  UpdateProjectRequest,
  UpdateProjectResponse,
  DeleteProjectRequest,
  DeleteProjectResponse,
  GenerateTokenResponse,
  JoinProjectRequest,
  JoinProjectResponse,
  LeaveProjectRequest,
  LeaveProjectResponse,
} from '../../types/project.types';

/**
 * ============================================
 * [GET] /project
 * 전체 프로젝트 목록을 조회합니다.
 * --------------------------------------------
 * @returns ProjectListResponse
 * - 프로젝트 배열 data 포함
 * ============================================
 */
export const fetchProjects = createAsyncThunk<ProjectListResponse, void>(
  'project/fetchProjects',
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.get<ProjectListResponse>('/project', {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = 'INTERNAL_ERROR';
      if (status === 400) errorCode = 'INVALID_REQUEST';
      else if (status === 401) errorCode = 'UNAUTHORIZED';

      return rejectWithValue({
        success: false,
        data: [],
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || '프로젝트 목록 조회 중 오류가 발생했습니다.',
        },
        timestamp: new Date().toISOString(),
      } as ProjectListResponse);
    }
  }
);

/**
 * ============================================
 * [POST] /project
 * 새로운 프로젝트를 생성합니다.
 * --------------------------------------------
 * @param projectData - 생성할 프로젝트 정보
 * @returns CreateProjectResponse
 * - 생성된 프로젝트 ID 포함
 * ============================================
 */
export const createProject = createAsyncThunk<CreateProjectResponse, CreateProjectRequest>(
  'project/createProject',
  async (projectData, { rejectWithValue }) => {
    try {
      const response = await api.post<CreateProjectResponse>('/project', projectData, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = 'INTERNAL_ERROR';
      if (status === 400) errorCode = 'INVALID_REQUEST';
      else if (status === 401) errorCode = 'UNAUTHORIZED';

      return rejectWithValue({
        success: false,
        data: { id: 0 },
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || '프로젝트 생성 중 오류가 발생했습니다.',
        },
        timestamp: new Date().toISOString(),
      } as CreateProjectResponse);
    }
  }
);

/**
 * ============================================
 * [PUT] /project/:projectId
 * 특정 프로젝트의 이름을 수정합니다.
 * --------------------------------------------
 * @param updateData - projectId와 새 이름
 * @returns UpdateProjectResponse
 * ============================================
 */
export const updateProject = createAsyncThunk<UpdateProjectResponse, UpdateProjectRequest>(
  'project/updateProject',
  async (updateData, { rejectWithValue }) => {
    try {
      const response = await api.put<UpdateProjectResponse>(
        `/project/${updateData.projectId}`,
        { name: updateData.name },
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
      else if (status === 404) errorCode = 'NOT_FOUND';

      return rejectWithValue({
        success: false,
        data: { id: 0 },
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || '프로젝트 수정 중 오류가 발생했습니다.',
        },
        timestamp: new Date().toISOString(),
      } as UpdateProjectResponse);
    }
  }
);

/**
 * ============================================
 * [DELETE] /project/:projectId
 * 프로젝트를 삭제합니다.
 * --------------------------------------------
 * @param projectId - 삭제할 프로젝트 ID
 * @returns DeleteProjectResponse
 * ============================================
 */
export const deleteProject = createAsyncThunk<DeleteProjectResponse, DeleteProjectRequest>(
  'project/deleteProject',
  async ({ projectId }, { rejectWithValue }) => {
    try {
      const response = await api.delete<DeleteProjectResponse>(`/project/${projectId}`, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = 'INTERNAL_ERROR';
      if (status === 400) errorCode = 'INVALID_REQUEST';
      else if (status === 401) errorCode = 'UNAUTHORIZED';

      return rejectWithValue({
        success: false,
        data: { id: 0 },
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || '프로젝트 삭제 중 오류가 발생했습니다.',
        },
        timestamp: new Date().toISOString(),
      } as DeleteProjectResponse);
    }
  }
);

/**
 * ============================================
 * [POST] /project/uuid
 * 프로젝트 초대 토큰을 생성합니다.
 * --------------------------------------------
 * @returns GenerateTokenResponse
 * - 토큰 문자열 포함
 * ============================================
 */
export const generateProjectToken = createAsyncThunk<GenerateTokenResponse, void>(
  'project/generateToken',
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.post<GenerateTokenResponse>('/project/uuid', null, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = 'INTERNAL_ERROR';
      if (status === 400) errorCode = 'INVALID_REQUEST';
      else if (status === 401) errorCode = 'UNAUTHORIZED';

      return rejectWithValue({
        success: false,
        data: { token: '' },
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || '토큰 생성 중 오류가 발생했습니다.',
        },
        timestamp: new Date().toISOString(),
      } as GenerateTokenResponse);
    }
  }
);

/**
 * ============================================
 * [POST] /api/project/join
 * 초대 토큰을 이용해 프로젝트에 참여합니다.
 * --------------------------------------------
 * @param requestData - 참여용 토큰 정보
 * @returns JoinProjectResponse
 * ============================================
 */
export const joinProject = createAsyncThunk<JoinProjectResponse, JoinProjectRequest>(
  'project/joinProject',
  async (requestData, { rejectWithValue }) => {
    try {
      const response = await api.post<JoinProjectResponse>('/api/project/join', requestData, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = 'INTERNAL_ERROR';
      if (status === 400) errorCode = 'INVALID_REQUEST';
      else if (status === 401) errorCode = 'UNAUTHORIZED';
      else if (status === 404) errorCode = 'NOT_FOUND';

      return rejectWithValue({
        success: false,
        data: {},
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || '프로젝트 참여 중 오류가 발생했습니다.',
        },
        timestamp: new Date().toISOString(),
      } as JoinProjectResponse);
    }
  }
);

/**
 * ============================================
 * [DELETE] /project/me
 * 현재 사용자가 프로젝트에서 탈퇴합니다.
 * --------------------------------------------
 * @param requestData - 탈퇴 대상 프로젝트 ID 등
 * @returns LeaveProjectResponse
 * ============================================
 */
export const leaveProject = createAsyncThunk<LeaveProjectResponse, LeaveProjectRequest>(
  'project/leaveProject',
  async (requestData, { rejectWithValue }) => {
    try {
      const response = await api.delete<LeaveProjectResponse>('/project/me', {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        data: requestData,
      });
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = 'INTERNAL_ERROR';
      if (status === 400) errorCode = 'INVALID_REQUEST';
      else if (status === 401) errorCode = 'UNAUTHORIZED';

      return rejectWithValue({
        success: false,
        data: {},
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || '프로젝트 탈퇴 중 오류가 발생했습니다.',
        },
        timestamp: new Date().toISOString(),
      } as LeaveProjectResponse);
    }
  }
);

/**
 * =========================
 * Project Slice & 상태 정의
 * =========================
 */

const initialState: ProjectState = {
  isLoading: false,
  error: null,
  projects: [],
};

const projectSlice = createSlice({
  name: 'project',
  initialState,
  reducers: {
    resetProjects: (state) => {
      state.projects = [];
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchProjects.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchProjects.fulfilled, (state, action) => {
        state.isLoading = false;
        state.projects = action.payload.data;
      })
      .addCase(fetchProjects.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as ProjectListResponse)?.error ?? {
          code: 'INTERNAL_ERROR',
          message: '알 수 없는 오류가 발생했습니다.',
        };
      })
      .addCase(createProject.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(createProject.fulfilled, (state) => {
        state.isLoading = false;
        state.error = null;
      })
      .addCase(createProject.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as CreateProjectResponse)?.error ?? {
          code: 'INTERNAL_ERROR',
          message: '알 수 없는 오류가 발생했습니다.',
        };
      })
      .addCase(updateProject.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(updateProject.fulfilled, (state, action) => {
        state.isLoading = false;
        state.error = null;
        const updatedId = action.payload.data.id;
        const updatedName = (action.meta as { arg: UpdateProjectRequest }).arg.name;
        const project = state.projects.find((p) => p.id === updatedId);
        if (project) project.name = updatedName;
      })
      .addCase(updateProject.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as UpdateProjectResponse)?.error ?? {
          code: 'INTERNAL_ERROR',
          message: '알 수 없는 오류가 발생했습니다.',
        };
      })
      .addCase(deleteProject.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(deleteProject.fulfilled, (state, action) => {
        state.isLoading = false;
        state.error = null;
        const deletedId = action.payload.data.id;
        state.projects = state.projects.filter((p) => p.id !== deletedId);
      })
      .addCase(deleteProject.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as DeleteProjectResponse)?.error ?? {
          code: 'INTERNAL_ERROR',
          message: '알 수 없는 오류가 발생했습니다.',
        };
      })
      .addCase(generateProjectToken.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(generateProjectToken.fulfilled, (state) => {
        state.isLoading = false;
        state.error = null;
      })
      .addCase(generateProjectToken.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as GenerateTokenResponse)?.error ?? {
          code: 'INTERNAL_ERROR',
          message: '알 수 없는 오류가 발생했습니다.',
        };
      })
      .addCase(joinProject.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(joinProject.fulfilled, (state) => {
        state.isLoading = false;
        state.error = null;
      })
      .addCase(joinProject.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as JoinProjectResponse)?.error ?? {
          code: 'INTERNAL_ERROR',
          message: '알 수 없는 오류가 발생했습니다.',
        };
      })
      .addCase(leaveProject.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(leaveProject.fulfilled, (state) => {
        state.isLoading = false;
        state.error = null;
      })
      .addCase(leaveProject.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as LeaveProjectResponse)?.error ?? {
          code: 'INTERNAL_ERROR',
          message: '알 수 없는 오류가 발생했습니다.',
        };
      });
  },
});

export const { resetProjects } = projectSlice.actions;
export default projectSlice.reducer;
