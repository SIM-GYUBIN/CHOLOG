/**
 * @description 프로젝트 관련 타입 정의 모음
 * @author Cholog FE Team
 */

/**
 * @description API 응답에 사용되는 에러 코드
 * @typedef {string} ErrorCode
 * @enum {string}
 * @property {"INVALID_REQUEST"} - 잘못된 요청
 * @property {"UNAUTHORIZED"} - 인증되지 않은 사용자
 * @property {"NOT_FOUND"} - 요청한 리소스를 찾을 수 없음
 * @property {"INTERNAL_ERROR"} - 서버 내부 오류
 */
type ErrorCode =
  | "INVALID_REQUEST"
  | "UNAUTHORIZED"
  | "NOT_FOUND"
  | "INTERNAL_ERROR";

/**
 * @description API 응답에 대한 기본 인터페이스
 */
interface BaseResponse {
  success: boolean;
  timestamp: string;
  error?: {
    code: ErrorCode;
    message: string;
  };
}

/**
 * [#PROJECT-1]
 * 프로젝트의 기본 정보를 담는 인터페이스
 * @property {number} id - 프로젝트의 고유 식별자
 * @property {string} name - 프로젝트의 이름
 * @property {string} projectToken - 프로젝트의 토큰 값
 * @property {boolean} isCreator - 현재 사용자가 프로젝트 생성자인지 여부
 * @property {string} createdAt - 프로젝트 생성 일시
 */
export interface Project {
  id: number;
  name: string;
  projectToken: string;
  isCreator: boolean;
  createdAt: string;
}

/**
 * [#PROJECT-1]
 * @description 프로젝트 목록 조회에 대한 API 응답 인터페이스
 * @extends {BaseResponse} 기본 API 응답 형식을 상속
 * @property {Project[]} data - 조회된 프로젝트 목록 배열
 */
export interface ProjectListResponse extends BaseResponse {
  data: {
    projects: Project[];
  };
}

/**
 * [#PROJECT-2]
 * @description 프로젝트를 새로 생성할 때 필요한 요청 데이터 인터페이스
 * @property {string} name - 생성할 프로젝트의 이름
 * @property {string} token - 프로젝트 생성 인증을 위한 토큰 값
 */
export interface CreateProjectRequest {
  name: string;
  token: string;
}

/**
 * [#PROJECT-2]
 * 프로젝트 생성 시 서버로부터 받는 응답 데이터 인터페이스
 * @extends {BaseResponse} 기본 API 응답 형식을 상속받음
 * @property {Object} data - 생성된 프로젝트의 정보
 * @property {number} data.id - 생성된 프로젝트의 고유 식별자
 */
export interface CreateProjectResponse extends BaseResponse {
  data: {
    id: number;
  };
}

/**
 * [#PROJECT-3]
 * @description 프로젝트 수정을 위한 요청 인터페이스
 * @property {number} projectId - 수정할 프로젝트의 고유 식별자
 * @property {string} name - 변경할 프로젝트의 새로운 이름
 */
export interface UpdateProjectRequest {
  projectId: number;
  name: string;
}

/**
 * [#PROJECT-3]
 * @description 프로젝트 수정 작업의 응답 인터페이스
 * @extends {BaseResponse} 기본 API 응답 형식을 상속
 * @property {Object} data - 수정된 프로젝트 정보
 * @property {number} data.id - 수정된 프로젝트의 식별자
 */
export interface UpdateProjectResponse extends BaseResponse {
  data: {
    id: number;
  };
}

/**
 * @description API 에러 응답 타입
 * @property {ErrorCode} code - 에러 코드
 * @property {string} message - 에러 메시지
 */
export interface ErrorResponse {
  code: ErrorCode;
  message: string;
}

/**
 * @description 프로젝트의 상태 관리를 위한 인터페이스
 * @property {boolean} isLoading - 데이터 로딩 상태 표시
 * @property {ErrorResponse | null} error - 발생한 오류 정보
 * @property {Project[]} projects - 프로젝트 목록 데이터
 */
export interface ProjectState {
  isLoading: boolean;
  error: ErrorResponse | null;
  projects: Project[];
}

/**
 * [#PROJECT-4]
 * @description 프로젝트 삭제 요청을 위한 인터페이스
 * @property {number} projectId - 삭제할 프로젝트의 식별자 (URL 경로 변수로 전달)
 */
export interface DeleteProjectRequest {
  projectId: number;
}

/**
 * [#PROJECT-4]
 * @description 프로젝트 삭제 작업의 응답 인터페이스
 * @extends {BaseResponse} 기본 API 응답 형식을 상속
 */
export interface DeleteProjectResponse extends BaseResponse {
  data: Record<string, never>;
}

/**
 * [#PROJECT-5]
 * @description 프로젝트 초대 토큰 생성 응답 인터페이스
 * @extends {BaseResponse} 기본 API 응답 형식을 상속
 * @property {Object} data - 생성된 토큰 정보
 * @property {string} data.token - 프로젝트 참여를 위한 고유 토큰
 */
export interface GenerateTokenResponse extends BaseResponse {
  data: {
    token: string;
  };
}

/**
 * [#PROJECT-6]
 * @description 프로젝트 참여 요청을 위한 인터페이스
 * @property {string} projectToken - 프로젝트 참여를 위한 인증 토큰
 */
export interface JoinProjectRequest {
  projectToken: string;
}

/**
 * [#PROJECT-6]
 * @description 프로젝트 참여 요청에 대한 응답 인터페이스
 * @extends {BaseResponse} 기본 API 응답 형식을 상속
 * @property {Record<string, never>} data - 빈 객체 응답
 */
export interface JoinProjectResponse extends BaseResponse {
  data: Record<string, never>;
}

/**
 * [#PROJECT-7]
 * @description 프로젝트 탈퇴 요청을 위한 인터페이스
 * @property {number} projectId - 탈퇴할 프로젝트의 식별자
 */
export interface LeaveProjectRequest {
  projectId: number;
}

/**
 * [#PROJECT-7]
 * @description 프로젝트 탈퇴 요청에 대한 응답 인터페이스
 * @extends {BaseResponse} 기본 API 응답 형식을 상속
 * @property {Record<number, never>} data - 빈 객체 응답
 */
export interface LeaveProjectResponse extends BaseResponse {
  data: Record<number, never>;
}

/**
 * [#PROJECT-8]
 * @description 프로젝트 상세 정보 조회 응답 인터페이스
 * @extends {BaseResponse} 기본 API 응답 형식을 상속
 * @property {Object} data - 프로젝트 상세 정보
 * @property {string} data.name - 프로젝트 이름
 * @property {string} data.projectToken - 프로젝트 토큰
 */
export interface ProjectDetailResponse extends BaseResponse {
  data: {
    name: string;
    projectToken: string;
  };
}
