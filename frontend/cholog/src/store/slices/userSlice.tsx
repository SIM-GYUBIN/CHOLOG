import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../../api/axios';
import { SignupRequest, ApiResponse, LoginRequest, LoginResponse, LogoutResponse, UserState } from '../../types/user.types';

/**
 * 회원 관련 api 슬라이스
 */

/**
 * 회원가입 api
 */
export const userSignup = createAsyncThunk<ApiResponse, SignupRequest>(
  'user/signup',
  async (userData: SignupRequest, { rejectWithValue }) => {
    try {
      const response = await api.post<ApiResponse>('/user', userData, {
        headers: {
          'Content-Type': 'application/json',
        },
      });
      return response.data;
    } catch (error: any) {
      return rejectWithValue({
        success: false,
        data: {},
        error: {
          code: error.response?.status === 400 ? 'INVALID_REQUEST' : 'INTERNAL_ERROR',
          message: error.response?.data?.error?.message || 'An error occurred',
        },
        timestamp: new Date().toISOString(),
      } as ApiResponse);
    }
  }
);

/**
 * 로그인 api
 */
export const userLogin = createAsyncThunk<LoginResponse, LoginRequest>(
  'user/login',
  async (credentials: LoginRequest, { rejectWithValue }) => {
    try {
      const response = await api.post<LoginResponse>('/user/login', credentials, {
        headers: {
          'Content-Type': 'application/json',
        },
      });
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = 'INTERNAL_ERROR';
      
      if (status === 400) errorCode = 'INVALID_REQUEST';
      else if (status === 401) errorCode = 'INVALID_CREDENTIALS';
      else if (status === 404) errorCode = 'ACCOUNT_NOT_FOUND';

      return rejectWithValue({
        success: false,
        data: {},
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || 'An error occurred',
        },
        timestamp: new Date().toISOString(),
      } as ApiResponse);
    }
  }
);

/**
 * 로그아웃 api
 */
export const userLogout = createAsyncThunk<LogoutResponse, void>(
  'user/logout',
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.post<LogoutResponse>('/user/logout', null, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
      });
      localStorage.removeItem('token');
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = 'INTERNAL_ERROR';
      
      if (status === 401) errorCode = 'UNAUTHORIZED';
      else if (status === 403) errorCode = 'FORBIDDEN';

      return rejectWithValue({
        success: false,
        data: {},
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || '로그아웃 중 오류가 발생했습니다.',
        },
        timestamp: new Date().toISOString(),
      } as ApiResponse);
    }
  }
);

const initialState: UserState = {
  isLoading: false,
  error: null,
  signupSuccess: false,
  nickname: null,
  isLoggedIn: false,
};

const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    resetSignupStatus: (state) => {
      state.signupSuccess = false;
      state.error = null;
    },
    logout: (state) => {
      state.isLoggedIn = false;
      state.nickname = null;
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(userSignup.pending, (state) => {
        state.isLoading = true;
        state.error = null;
        state.signupSuccess = false;
      })
      .addCase(userSignup.fulfilled, (state) => {
        state.isLoading = false;
        state.error = null;
        state.signupSuccess = true;
      })
      .addCase(userSignup.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as ApiResponse)?.error || null;
        state.signupSuccess = false;
      })
      // Login cases
      .addCase(userLogin.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(userLogin.fulfilled, (state, action) => {
        state.isLoading = false;
        state.error = null;
        state.isLoggedIn = true;
        state.nickname = action.payload.data.nickname;
      })
      .addCase(userLogin.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as ApiResponse)?.error || null;
        state.isLoggedIn = false;
        state.nickname = null;
      })
      // Logout cases
      .addCase(userLogout.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(userLogout.fulfilled, (state) => {
        state.isLoading = false;
        state.error = null;
        state.isLoggedIn = false;
        state.nickname = null;
      })
      .addCase(userLogout.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as ApiResponse)?.error || null;
      });
  },
});

export const { resetSignupStatus, logout } = userSlice.actions;
export default userSlice.reducer;