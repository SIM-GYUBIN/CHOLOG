import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import api from "../../api/axios";
import {
  SignupRequest,
  ApiResponse,
  LoginRequest,
  LoginResponse,
  LogoutResponse,
  UserState,
} from "../../types/user.types";

/**
 * [#USER-1]
 * @description 사용자 회원가입 비동기 API 호출
 * @param userData - 사용자 회원가입 요청 데이터
 * @returns ApiResponse 형태의 응답 또는 에러 정보
 */
export const userSignup = createAsyncThunk<ApiResponse, SignupRequest>(
  "user/signup",
  async (userData, { rejectWithValue }) => {
    try {
      const response = await api.post<ApiResponse>("/user", userData, {
        headers: {
          "Content-Type": "application/json",
        },
      });
      return response.data;
    } catch (error: any) {
      return rejectWithValue({
        success: false,
        data: {},
        error: {
          code:
            error.response?.status === 400
              ? "INVALID_REQUEST"
              : "INTERNAL_ERROR",
          message: error.response?.data?.error?.message || "An error occurred",
        },
        timestamp: new Date().toISOString(),
      } as ApiResponse);
    }
  }
);

/**
 * [#USER-2]
 * @description 사용자 로그인 비동기 API 호출
 * @param credentials - 로그인 요청 데이터 (이메일, 비밀번호 등)
 * @returns LoginResponse 형태의 응답 또는 에러 정보
 */
export const userLogin = createAsyncThunk<LoginResponse, LoginRequest>(
  "user/login",
  async (credentials, { rejectWithValue }) => {
    try {
      const response = await api.post<LoginResponse>(
        "/user/login",
        credentials,
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
      else if (status === 401) errorCode = "INVALID_CREDENTIALS";
      else if (status === 404) errorCode = "ACCOUNT_NOT_FOUND";

      return rejectWithValue({
        success: false,
        data: {},
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || "An error occurred",
        },
        timestamp: new Date().toISOString(),
      } as ApiResponse);
    }
  }
);

/**
 * [#USER-3]
 * @description 사용자 로그아웃 비동기 API 호출
 * @returns LogoutResponse 형태의 응답 또는 에러 정보
 */
export const userLogout = createAsyncThunk<LogoutResponse, void>(
  "user/logout",
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.post<LogoutResponse>("/user/logout", null, {
        headers: {
          "Content-Type": "application/json",
        },
      });
      localStorage.removeItem("token");
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = "INTERNAL_ERROR";

      if (status === 401) errorCode = "UNAUTHORIZED";
      else if (status === 403) errorCode = "FORBIDDEN";

      return rejectWithValue({
        success: false,
        data: {},
        error: {
          code: errorCode,
          message:
            error.response?.data?.error?.message ||
            "로그아웃 중 오류가 발생했습니다.",
        },
        timestamp: new Date().toISOString(),
      } as ApiResponse);
    }
  }
);

//
export const getCurrentUser = createAsyncThunk<ApiResponse, void>(
  "user/getCurrentUser",
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.get<ApiResponse>("/user");
      return response.data;
    } catch (error: any) {
      return rejectWithValue({
        success: false,
        data: {},
        error: {
          code: "UNAUTHORIZED",
          message: "로그인이 필요합니다.",
        },
        timestamp: new Date().toISOString(),
      } as ApiResponse);
    }
  }
);

/**
 * ==========================
 * User Slice & State 설정
 * ==========================
 */

// 초기 상태 정의
const initialState: UserState = {
  isLoading: false,
  error: null,
  signupSuccess: false,
  isLoggedIn: false,
  isAuthChecked: false,
};

// userSlice 생성
const userSlice = createSlice({
  name: "user",
  initialState,
  reducers: {
    /**
     * 회원가입 상태 초기화 (성공 여부 및 에러 제거)
     */
    resetSignupStatus: (state) => {
      state.signupSuccess = false;
      state.error = null;
    },
    /**
     * 로컬 상태에서 사용자 로그아웃 처리 (store 초기화)
     */
    logout: (state) => {
      state.isLoggedIn = false;
      state.error = null;
    },
    resetLoginStatus: (state) => {
      state.isLoggedIn = false;
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // 회원가입
      .addCase(userSignup.pending, (state) => {
        state.isLoading = true;
        state.error = null;
        state.signupSuccess = false;
      })
      .addCase(userSignup.fulfilled, (state) => {
        state.isLoading = false;
        state.signupSuccess = true;
        state.error = null;
      })
      .addCase(userSignup.rejected, (state, action) => {
        state.isLoading = false;
        state.signupSuccess = false;
        state.error = (action.payload as ApiResponse)?.error || null;
      })

      // 로그인
      .addCase(userLogin.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(userLogin.fulfilled, (state, action) => {
        state.isLoading = false;
        state.isLoggedIn = true;
        state.error = null;
      })
      .addCase(userLogin.rejected, (state, action) => {
        state.isLoading = false;
        state.isLoggedIn = false;
        state.error = (action.payload as ApiResponse)?.error || null;
      })

      // 로그아웃
      .addCase(userLogout.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(userLogout.fulfilled, (state) => {
        state.isLoading = false;
        state.isLoggedIn = false;
        state.error = null;
      })
      .addCase(userLogout.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as ApiResponse)?.error || null;
      })
      .addCase(getCurrentUser.fulfilled, (state) => {
        state.isLoggedIn = true;
        state.isAuthChecked = true;
      })
      .addCase(getCurrentUser.rejected, (state) => {
        state.isLoggedIn = false;
        state.isAuthChecked = true;
      });
  },
});

// 액션 및 리듀서 내보내기
export const { resetSignupStatus, logout, resetLoginStatus } =
  userSlice.actions;
export default userSlice.reducer;
