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
 * [#EXTRA_FEATURES-3]
 * @description 웹훅 설정 조회 응답
 * @extends {BaseResponse}
 * @property {Object} data - 응답 데이터
 * @property {boolean} data.exists - 설정 존재 여부
 * @property {Object} data.webhookSetting - 설정 정보
 * @property {number} data.webhookSetting.id - 설정 ID
 * @property {string} data.webhookSetting.mmURL - Mattermost URL
 * @property {string} data.webhookSetting.logLevel - 로그 레벨
 * @property {string} data.webhookSetting.notificationENV - 알림 개발환경
 * @property {boolean} data.webhookSetting.isEnabled - 알림 활성화 여부
 */
export interface GetWebhookSettingResponse extends BaseResponse {
  data: {
    exists: boolean;
    webhookSetting: {
      id: number;
      mmURL: string;
      logLevel: string;
      notificationENV: string;
      isEnabled: boolean;
    } | null;
  };
}

/**
 * [#EXTRA_FEATURES-4]
 * @description 웹훅 설정 생성 요청
 * @property {number} projectId - 프로젝트 ID
 * @property {string} mmURL - Mattermost URL
 * @property {string} logLevel - 로그 레벨 (예: ERROR, WARN, INFO)
 * @property {string} [notificationENV] - 알림 받을 환경 (예: prod, local)
 * @property {boolean} isEnabled - 알림 활성화 여부
 */
export interface CreateWebhookSettingRequest {
  projectId: number;
  mmURL: string;
  logLevel: string;
  notificationENV?: string;
  isEnabled: boolean;
}

/**
 * [#EXTRA_FEATURES-4]
 * @description 웹훅 설정 생성 응답
 * @extends {BaseResponse}
 * @property {Object} data - 응답 데이터 없음
 */
export interface CreateWebhookSettingResponse extends BaseResponse {
  data: {};
}

/**
 * [#EXTRA_FEATURES-5]
 * @description 웹훅 설정 수정 요청
 * @property {number} projectId - 프로젝트 ID
 * @property {string} mmURL - Mattermost URL
 * @property {string} logLevel - 로그 레벨
 * @property {string} [notificationENV] - 알림 받을 환경
 * @property {boolean} isEnabled - 알림 활성화 여부
 */
export interface UpdateWebhookSettingRequest {
  projectId: number;
  mmURL: string;
  logLevel: string;
  notificationENV?: string;
  isEnabled: boolean;
}

/**
 * [#EXTRA_FEATURES-5]
 * @description 웹훅 설정 수정 응답
 * @extends {BaseResponse}
 * @property {Object} data - 응답 데이터
 * @property {string} data.id - 수정된 설정의 ID
 */
export interface UpdateWebhookSettingResponse extends BaseResponse {
  data: {
    id: string;
  };
}

/**
 * @description ExtraFeatures 상태
 * @property {boolean} isLoading - 로딩 상태
 * @property {BaseResponse["error"] | null} error - 에러 정보
 * @property {GetJiraTokenResponse["data"] | null} jiraToken - JIRA 토큰 정보
 * @property {GetWebhookSettingResponse["data"] | null} webhookSetting - 웹훅 설정 정보
 */
export interface ExtraFeaturesState {
  isLoading: boolean;
  error: BaseResponse["error"] | null;
  jiraToken: GetJiraTokenResponse["data"] | null;
  webhookSetting: GetWebhookSettingResponse["data"] | null;
}
