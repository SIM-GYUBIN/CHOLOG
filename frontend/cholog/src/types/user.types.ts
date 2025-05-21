/**
 * @description 유저 관련 타입 정의 모음
 * @author Cholog FE Team
 */

/**
 * 에러 코드 타입
 */
export type ErrorCode =
  | "INVALID_REQUEST"
  | "UNAUTHORIZED"
  | "FORBIDDEN"
  | "INTERNAL_ERROR"
  | "INVALID_CREDENTIALS"
  | "ACCOUNT_NOT_FOUND"
  | "INVALID_RESPONSE";

/**
 * 에러 객체 타입
 */
export interface ApiError {
  code: ErrorCode;
  message: string;
}

/**
 * [#USER-1]
 * @description API 응답에 사용되는 에러 코드
 * @property {boolean} success - 요청 성공 여부
 * @property {Record<string, unknown>} data - 응답 데이터
 * @property {string} timestamp - 응답 시간
 * @property {Object} error - 에러 정보
 * @property {ErrorCode} error.code - 에러 코드
 * @property {string} error.message - 에러 메시지
 */
export interface ApiResponse {
  success: boolean;
  data: Record<string, unknown>;
  timestamp: string;
  error?: ApiError;
}

/**
 * [#USER-1]
 * @description 회원가입 요청 데이터
 * @property {string} email - 이메일
 * @property {string} password - 비밀번호
 * @property {string} nickname - 닉네임
 */
export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
}

/**
 * [#USER-2]
 * @description 로그인 요청 데이터
 * @property {string} email - 이메일
 * @property {string} password - 비밀번호
 */
export interface LoginRequest {
  email: string;
  password: string;
}

/**
 * [#USER-2]
 * @description 로그인 응답 데이터
 * @extends {ApiResponse} API 응답 형식 확장
 * @property {Object} data - 응답 데이터
 * @property {string} data.nickname - 로그인한 사용자의 닉네임
 */
export interface LoginResponse {
  success: boolean;
  data: {
    nickname: string;
  };
  timestamp: string;
  error?: ApiError;
}

/**
 * [#USER-3]
 * @description 로그아웃 응답 데이터
 * @extends {ApiResponse} API 응답 형식 확장
 * @property {Object} data - 응답 데이터
 * @property {Record<string, never>} data - 응답 데이터가 없음
 */
export interface LogoutResponse extends ApiResponse {
  data: Record<string, never>;
}

export interface UserState {
  isLoading: boolean;
  error: ApiError | null;
  signupSuccess: boolean;
  isLoggedIn: boolean;
  isAuthChecked: boolean;
}
