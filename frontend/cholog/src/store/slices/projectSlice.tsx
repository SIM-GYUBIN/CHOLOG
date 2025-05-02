import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import api from '../../api/axios';
import {
  ProjectListResponse,
  ProjectState,
  Project,
  CreateProjectRequest,
  CreateProjectResponse,
  UpdateProjectRequest,
  UpdateProjectResponse,
} from '../../types/project.types';

/**
 * ================================
 * Project 관련 비동기 Thunk 정의
 * ================================
 */

/**
 * @description 전체 프로젝트 목록을 조회하는 API 호출
 * @returns ProjectListResponse 형태의 데이터 또는 에러 정보
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
        data: [] as Project[],
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
 * @description 새로운 프로젝트를 생성하는 API 호출
 * @param projectData - 생성할 프로젝트의 정보
 * @returns CreateProjectResponse 형태의 결과 또는 에러 정보
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
 * @description 기존 프로젝트 이름을 수정하는 API 호출
 * @param updateData - 수정할 프로젝트 ID 및 새로운 이름
 * @returns UpdateProjectResponse 형태의 결과 또는 에러 정보
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
    /**
     * @description 프로젝트 목록 및 에러 상태 초기화
     */
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
      .addCase(fetchProjects.fulfilled, (state, action: PayloadAction<ProjectListResponse>) => {
        state.isLoading = false;
        state.projects = action.payload.data;
        state.error = null;
      })
      .addCase(fetchProjects.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload
          ? (action.payload as ProjectListResponse).error
          : { code: 'INTERNAL_ERROR', message: '알 수 없는 오류가 발생했습니다.' };
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
        state.error = action.payload
          ? (action.payload as CreateProjectResponse).error
          : { code: 'INTERNAL_ERROR', message: '알 수 없는 오류가 발생했습니다.' };
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
        if (project) {
          project.name = updatedName;
        }
      })
      .addCase(updateProject.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload
          ? (action.payload as UpdateProjectResponse).error
          : { code: 'INTERNAL_ERROR', message: '알 수 없는 오류가 발생했습니다.' };
      });
  },
});

export const { resetProjects } = projectSlice.actions;
export default projectSlice.reducer;
