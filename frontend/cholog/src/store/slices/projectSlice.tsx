import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import api from "../../api/axios";
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
  ProjectDetailResponse, // 이 부분 추가
} from "../../types/project.types";

// 목데이터 import 제거

/**
 * ============================================
 * [#PROJECT-1]
 * [GET] /project
 * 전체 프로젝트 목록을 조회합니다.
 * --------------------------------------------
 * @returns ProjectListResponse
 * - 프로젝트 배열 data 포함
 * ============================================
 */
export const fetchProjects = createAsyncThunk<ProjectListResponse, void>(
  "project/fetchProjects",
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.get<ProjectListResponse>("/project", {
        headers: {
          "Content-Type": "application/json",
        },
      });
      return response.data;
    } catch (error: any) {
      // 목데이터 반환 대신 에러 처리
      const status = error.response?.status;
      let errorCode = "INTERNAL_ERROR";
      if (status === 400) errorCode = "INVALID_REQUEST";
      else if (status === 401) errorCode = "UNAUTHORIZED";
      
      return rejectWithValue({
        success: false,
        data: { projects: [] },
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || "프로젝트 목록을 불러오는 중 오류가 발생했습니다.",
        },
        timestamp: new Date().toISOString(),
      } as ProjectListResponse);
    }
  }
);

/**
 * ============================================
 * [#PROJECT-2]
 * [POST] /project
 * 새로운 프로젝트를 생성합니다.
 * --------------------------------------------
 * @param projectData - 생성할 프로젝트 정보
 * @returns CreateProjectResponse
 * - 생성된 프로젝트 ID 포함
 * ============================================
 */
export const createProject = createAsyncThunk<
  CreateProjectResponse,
  CreateProjectRequest
>("project/createProject", async (projectData, { rejectWithValue }) => {
  try {
    const response = await api.post<CreateProjectResponse>(
      "/project",
      projectData,
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
      data: { id: 0 },
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "프로젝트 생성 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as CreateProjectResponse);
  }
});

/**
 * ============================================
 * [#PROJECT-3]
 * [PUT] /project/:projectId
 * 특정 프로젝트의 이름을 수정합니다.
 * --------------------------------------------
 * @param updateData - projectId와 새 이름
 * @returns UpdateProjectResponse
 * ============================================
 */
export const updateProject = createAsyncThunk<
  UpdateProjectResponse,
  UpdateProjectRequest
>("project/updateProject", async (updateData, { rejectWithValue }) => {
  try {
    const response = await api.put<UpdateProjectResponse>(
      `/project/${updateData.projectId}`,
      { name: updateData.name },
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
      data: { id: 0 },
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "프로젝트 수정 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as UpdateProjectResponse);
  }
});

/**
 * ============================================
 * [#PROJECT-4]
 * [DELETE] /project/:projectId
 * 프로젝트를 삭제합니다.
 * --------------------------------------------
 * @param projectId - 삭제할 프로젝트 ID
 * @returns DeleteProjectResponse
 * ============================================
 */
export const deleteProject = createAsyncThunk<
  DeleteProjectResponse,
  DeleteProjectRequest
>("project/deleteProject", async ({ projectId }, { rejectWithValue }) => {
  try {
    const response = await api.delete<DeleteProjectResponse>(
      `/project/${projectId}`,
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
      data: {},
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "프로젝트 삭제 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as DeleteProjectResponse);
  }
});

/**
 * ============================================
 * [#PROJECT-5]
 * [POST] /project/uuid
 * 프로젝트 초대 토큰을 생성합니다.
 * --------------------------------------------
 * @returns GenerateTokenResponse
 * - 토큰 문자열 포함
 * ============================================
 */
export const generateProjectToken = createAsyncThunk<
  GenerateTokenResponse,
  void
>("project/generateToken", async (_, { rejectWithValue }) => {
  try {
    const response = await api.post<GenerateTokenResponse>(
      "/project/uuid",
      null,
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
      data: { token: "" },
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "토큰 생성 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as GenerateTokenResponse);
  }
});

/**
 * ============================================
 * [#PROJECT-6]
 * [POST] /api/project/join
 * 초대 토큰을 이용해 프로젝트에 참여합니다.
 * --------------------------------------------
 * @param requestData - 참여용 토큰 정보
 * @returns JoinProjectResponse
 * ============================================
 */
export const joinProject = createAsyncThunk<
  JoinProjectResponse,
  JoinProjectRequest
>("project/joinProject", async (requestData, { rejectWithValue }) => {
  try {
    const response = await api.post<JoinProjectResponse>(
      "/project/join",
      { token: requestData.projectToken }, // 여기를 수정: token 필드로 전달
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
      data: {},
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "프로젝트 참여 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as JoinProjectResponse);
  }
});

/**
 * ============================================
 * [#PROJECT-7]
 * [DELETE] /project/me
 * 현재 사용자가 프로젝트에서 탈퇴합니다.
 * --------------------------------------------
 * @param requestData - 탈퇴 대상 프로젝트 ID 등
 * @returns LeaveProjectResponse
 * ============================================
 */
export const leaveProject = createAsyncThunk<
  LeaveProjectResponse,
  LeaveProjectRequest
>("project/leaveProject", async (requestData, { rejectWithValue }) => {
  try {
    const response = await api.delete<LeaveProjectResponse>(
      `/project/${requestData.projectId}/me`,  // URL에 projectId를 포함
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
      data: {},
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "프로젝트 탈퇴 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as LeaveProjectResponse);
  }
});

/**
 * ============================================
 * [#PROJECT-8]
 * [GET] /project/:projectId
 * 프로젝트의 상세 정보를 조회합니다.
 * --------------------------------------------
 * @param projectId - 조회할 프로젝트 ID
 * @returns ProjectDetailResponse
 * - 프로젝트 이름과 토큰 정보 포함
 * ============================================
 */
export const fetchProjectDetail = createAsyncThunk<
  ProjectDetailResponse,
  number
>("project/fetchProjectDetail", async (projectId, { rejectWithValue }) => {
  try {
    const response = await api.get<ProjectDetailResponse>(
      `/project/${projectId}`,
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
      data: {
        name: "",
        projectToken: "",
      },
      error: {
        code: errorCode,
        message:
          error.response?.data?.error?.message ||
          "프로젝트 정보 조회 중 오류가 발생했습니다.",
      },
      timestamp: new Date().toISOString(),
    } as ProjectDetailResponse);
  }
});

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
  name: "project",
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
        state.projects = action.payload.data.projects;
      })
      .addCase(fetchProjects.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as ProjectListResponse)?.error ?? {
          code: "INTERNAL_ERROR",
          message: "알 수 없는 오류가 발생했습니다.",
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
          code: "INTERNAL_ERROR",
          message: "알 수 없는 오류가 발생했습니다.",
        };
      })
      .addCase(updateProject.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(updateProject.fulfilled, (state, action) => {
        state.isLoading = false;
        state.error = null;
        const updatedId = action.payload.data?.id;
        const updatedName = (action.meta as { arg: UpdateProjectRequest }).arg
          .name;
        const project = state.projects.find((p) => p.id === updatedId);
        if (project) project.name = updatedName;
      })
      .addCase(updateProject.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as UpdateProjectResponse)?.error ?? {
          code: "INTERNAL_ERROR",
          message: "알 수 없는 오류가 발생했습니다.",
        };
      })
      .addCase(deleteProject.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(deleteProject.fulfilled, (state, action) => {
        state.isLoading = false;
        state.error = null;
        // 삭제된 프로젝트 ID는 action.meta.arg에서 가져옵니다
        const deletedId = (action.meta.arg as DeleteProjectRequest).projectId;
        state.projects = state.projects.filter((p) => p.id !== deletedId);
      })
      .addCase(deleteProject.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as DeleteProjectResponse)?.error ?? {
          code: "INTERNAL_ERROR",
          message: "알 수 없는 오류가 발생했습니다.",
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
          code: "INTERNAL_ERROR",
          message: "알 수 없는 오류가 발생했습니다.",
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
          code: "INTERNAL_ERROR",
          message: "알 수 없는 오류가 발생했습니다.",
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
          code: "INTERNAL_ERROR",
          message: "알 수 없는 오류가 발생했습니다.",
        };
      })
      .addCase(fetchProjectDetail.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchProjectDetail.fulfilled, (state, action) => {
        state.isLoading = false;
        // Update the project in the existing projects array
        const projectDetail = {
          id: Number(action.meta.arg), // projectId from the action
          name: action.payload.data.name,
          projectToken: action.payload.data.projectToken,
          createdAt: new Date().toISOString(), // Since we don't get this from detail API
          isCreator: true // Since we don't get this from detail API
        };
        
        // Find and update existing project or add as new
        const existingIndex = state.projects.findIndex(p => p.id === projectDetail.id);
        if (existingIndex >= 0) {
          state.projects[existingIndex] = projectDetail;
        } else {
          state.projects.push(projectDetail);
        }
        state.error = null;
      })
      .addCase(fetchProjectDetail.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as ProjectDetailResponse)?.error ?? {
          code: "INTERNAL_ERROR",
          message: "알 수 없는 오류가 발생했습니다.",
        };
      });
  },
});

export const { resetProjects } = projectSlice.actions;
export default projectSlice.reducer;
