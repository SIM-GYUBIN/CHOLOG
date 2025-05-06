/**
 * 유저 데이터 타입 규정
 */

// Common API Response Type
export interface ApiResponse {
  success: boolean;
  data: Record<string, unknown>;
  timestamp: string;
  error?: {
    code:
      | "INVALID_REQUEST"
      | "UNAUTHORIZED"
      | "FORBIDDEN"
      | "INTERNAL_ERROR"
      | "INVALID_CREDENTIALS"
      | "ACCOUNT_NOT_FOUND";
    message: string;
  };
}

// Signup Types
export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
}

// Login Types
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse extends ApiResponse {
  data: {
    nickname: string;
  };
}

// Logout Types
export interface LogoutResponse extends ApiResponse {
  data: Record<string, never>;
}

export interface UserState {
  isLoading: boolean;
  error: ApiResponse["error"] | null;
  signupSuccess: boolean;
  nickname: string | null;
  isLoggedIn: boolean;
}
