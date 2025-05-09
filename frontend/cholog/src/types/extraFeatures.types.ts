/**
 * @description API 응답에 사용되는 에러 코드
 * @property {boolean} success - 요청 성공 여부
 * @property {string} timestamp - 응답 시간
 * @property {Object} error - 에러 정보
 * @property {ErrorCode} error.code - 에러 코드
 * @property {string} error.message - 에러 메시지
 */
interface BaseResponse {
  success: boolean;
  timestamp: string;
  error?: {
    code: "INVALID_REQUEST" | "UNAUTHORIZED" | "INTERNAL_ERROR";
    message: string;
  };
}

/**
 * [#EXTRA_FEATURES-1]
 * @description JIRA 토큰 등록/수정 요청
 * @property {number} projectId - 프로젝트 ID
 * @property {string} token - JIRA 토큰
 */
export interface UpdateJiraTokenRequest {
  projectId: number;
  token: string;
}

/**
 * [#EXTRA_FEATURES-1]
 * @description JIRA 토큰 등록/수정 응답
 * @extends {BaseResponse}
 * @property {Object} data - 응답 데이터
 * @property {number} data.projectId - 프로젝트 ID
 */
export interface UpdateJiraTokenResponse extends BaseResponse {
  data: {
    projectId: number;
  };
}

/**
 * [#EXTRA_FEATURES-2]
 * @description JIRA 토큰 조회 요청
 * @property {number} projectId - 프로젝트 ID
 * @property {string} token - JIRA 토큰
 */
export interface GetJiraTokenResponse extends BaseResponse {
  data: {
    projectId: number;
    token: string;
  };
}

/**
 * @description ExtraFeatures 상태
 * @property {boolean} isLoading - 로딩 상태
 * @property {BaseResponse["error"] | null} error - 에러 정보
 * @property {GetJiraTokenResponse["data"] | null} jiraToken - JIRA 토큰 정보
 */
export interface ExtraFeaturesState {
  isLoading: boolean;
  error: BaseResponse["error"] | null;
  jiraToken: GetJiraTokenResponse["data"] | null;
}