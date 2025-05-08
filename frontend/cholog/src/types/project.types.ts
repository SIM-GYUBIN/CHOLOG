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

/**
 * 프로젝트의 기본 정보를 담는 인터페이스
 * @property {number} id - 프로젝트의 고유 식별자
 * @property {string} name - 프로젝트의 이름
 * @property {boolean} isCreator - 현재 사용자가 프로젝트 생성자인지 여부
 * @property {string} createdAt - 프로젝트 생성 일시
 */
export interface Project {
  id: number;
  name: string;
  isCreator: boolean;
  createdAt: string;
}

/**
 * 프로젝트 목록 조회에 대한 API 응답 인터페이스
 * @extends {BaseResponse} 기본 API 응답 형식을 상속
 * @property {Project[]} data - 조회된 프로젝트 목록 배열
 * @description
 * - 서버로부터 받아온 프로젝트 목록 데이터를 담는 응답 타입
 * - 프로젝트 목록 조회 API 엔드포인트에서 반환되는 응답 구조
 */
export interface ProjectListResponse extends BaseResponse {
  data: Project[];
}

/**
 * 프로젝트를 새로 생성할 때 필요한 요청 데이터 인터페이스
 * @property {string} name - 생성할 프로젝트의 이름
 * @property {string} token - 프로젝트 생성 인증을 위한 토큰 값
 */
export interface CreateProjectRequest {
  name: string;
  token: string;
}

/**
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
 * 프로젝트 수정을 위한 요청 인터페이스
 * @property {number} projectId - 수정할 프로젝트의 고유 식별자
 * @property {string} name - 변경할 프로젝트의 새로운 이름
 */
export interface UpdateProjectRequest {
  projectId: number;
  name: string;
}

/**
 * 프로젝트 수정 작업의 응답 인터페이스
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
 * 프로젝트의 상태 관리를 위한 인터페이스
 * @property {boolean} isLoading - 데이터 로딩 상태 표시
 * @property {BaseResponse["error"] | null} error - 발생한 오류 정보
 * @property {Project[]} projects - 프로젝트 목록 데이터
 */
export interface ProjectState {
  isLoading: boolean;
  error: BaseResponse["error"] | null;
  projects: Project[];
}

/**
 * 프로젝트 삭제 요청을 위한 인터페이스
 * @property {number} projectId - 삭제할 프로젝트의 식별자 (URL 경로 변수로 전달)
 */
export interface DeleteProjectRequest {
  projectId: number;
}

/**
 * 프로젝트 삭제 작업의 응답 인터페이스
 * @extends {BaseResponse} 기본 API 응답 형식을 상속
 * @property {Object} data - 삭제된 프로젝트 정보
 * @property {number} data.id - 삭제된 프로젝트의 식별자
 */
export interface DeleteProjectResponse extends BaseResponse {
  data: {
    id: number;
  };
}

/**
 * 프로젝트 초대 토큰 생성 응답 인터페이스
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
 * 프로젝트 참여 요청을 위한 인터페이스
 * @property {string} token - 프로젝트 참여를 위한 인증 토큰
 */
export interface JoinProjectRequest {
  token: string;
}

/**
 * 프로젝트 참여 요청에 대한 응답 인터페이스
 * @extends {BaseResponse} 기본 API 응답 형식을 상속
 * @property {Record<string, never>} data - 빈 객체 응답
 */
export interface JoinProjectResponse extends BaseResponse {
  data: Record<string, never>;
}

/**
 * 프로젝트 탈퇴 요청을 위한 인터페이스
 * @property {string} projectId - 탈퇴할 프로젝트의 식별자
 */
export interface LeaveProjectRequest {
  projectId: string;
}

/**
 * 프로젝트 탈퇴 요청에 대한 응답 인터페이스
 * @extends {BaseResponse} 기본 API 응답 형식을 상속
 * @property {Record<string, never>} data - 빈 객체 응답
 */
export interface LeaveProjectResponse extends BaseResponse {
  data: Record<string, never>;
}
