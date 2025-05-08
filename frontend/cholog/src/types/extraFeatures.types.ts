// 기본 응답 타입
interface BaseResponse {
  success: boolean;
  timestamp: string;
  error?: {
    code: "INVALID_REQUEST" | "UNAUTHORIZED" | "INTERNAL_ERROR";
    message: string;
  };
}

// JIRA 토큰 등록/수정 요청
export interface UpdateJiraTokenRequest {
  projectId: number;
  token: string;
}

// JIRA 토큰 등록/수정 응답
export interface UpdateJiraTokenResponse extends BaseResponse {
  data: {
    projectId: number;
  };
}

// JIRA 토큰 조회 응답
export interface GetJiraTokenResponse extends BaseResponse {
  data: {
    projectId: number;
    token: string;
  };
}

// extraFeatures 상태 타입
export interface ExtraFeaturesState {
  isLoading: boolean;
  error: BaseResponse["error"] | null;
  jiraToken: GetJiraTokenResponse["data"] | null;
}