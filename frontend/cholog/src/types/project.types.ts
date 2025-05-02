/**
 * 프로젝트 데이터 타입 규정
 */

// 기본 에러 타입
type ErrorCode =
  | "INVALID_REQUEST"
  | "UNAUTHORIZED"
  | "NOT_FOUND"
  | "INTERNAL_ERROR";

// 기본 API 응답 타입
interface BaseResponse {
  success: boolean;
  timestamp: string;
  error?: {
    code: ErrorCode;
    message: string;
  };
}

// 프로젝트 기본 타입
export interface Project {
  id: number;
  name: string;
}

// 프로젝트 목록 조회 응답
export interface ProjectListResponse extends BaseResponse {
  data: Project[];
}

// 프로젝트 생성 요청
export interface CreateProjectRequest {
  name: string;
  token: string;
}

// 프로젝트 생성 응답
export interface CreateProjectResponse extends BaseResponse {
  data: {
    id: number;
  };
}

// 프로젝트 수정 요청
export interface UpdateProjectRequest {
  projectId: number;
  name: string;
}

// 프로젝트 수정 응답
export interface UpdateProjectResponse extends BaseResponse {
  data: {
    id: number;
  };
}

// 프로젝트 상태
export interface ProjectState {
  isLoading: boolean;
  error: BaseResponse["error"] | null;
  projects: Project[];
}

// 삭제 요청: projectId만 필요 (PathVariable로 전달)
export interface DeleteProjectRequest {
  projectId: number;
}

// 삭제 응답
export interface DeleteProjectResponse extends BaseResponse {
  data: {
    id: number;
  };
}

// 프로젝트 토큰 생성 응답
export interface GenerateTokenResponse extends BaseResponse {
  data: {
    token: string;
  };
}

// 프로젝트 참여 요청
export interface JoinProjectRequest {
  token: string;
}

// 프로젝트 참여 응답
export interface JoinProjectResponse extends BaseResponse {
  data: Record<string, never>; // 빈 객체
}

// 프로젝트 탈퇴 요청
export interface LeaveProjectRequest {
  projectId: string;
}

// 프로젝트 탈퇴 응답
export interface LeaveProjectResponse extends BaseResponse {
  data: Record<string, never>; // {}
}
