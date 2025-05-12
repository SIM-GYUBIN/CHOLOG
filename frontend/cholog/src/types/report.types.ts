/**
 * @description 리포트 관련 타입 정의 모음
 * @author Cholog FE Team
 */

/**
 * [#REPORT-1]
 * @description 리포트 조회를 위한 경로 변수 인터페이스
 * @property {number} projectId - 프로젝트의 고유 식별자
 * @property {string} reportId - 리포트의 고유 식별자
 */
export interface ReportPathParams {
  projectId: number;
  reportId: string;
}

/**
 * [#REPORT-2]
 * @description 리포트 섹션의 기본 구조를 정의하는 인터페이스
 * @property {string} sectionTitle - 섹션의 제목
 * @property {string} content - 섹션의 내용
 */
export interface ReportSection {
  sectionTitle: string;
  content: string;
}

/**
 * [#REPORT-3]
 * @description 리포트의 상세 정보를 담는 인터페이스
 * @property {string} reportId - 리포트의 고유 식별자
 * @property {number} projectId - 리포트가 속한 프로젝트의 식별자
 * @property {string} title - 리포트의 제목
 * @property {string} summary - 리포트의 요약 내용
 * @property {string} createdAt - 리포트 생성 시각 (ISO 8601 포맷)
 * @property {string} createdBy - 리포트 작성자 이메일
 * @property {ReportSection[]} sections - 리포트의 섹션 목록
 */
export interface Report {
  reportId: string;
  projectId: number;
  title: string;
  summary: string;
  createdAt: string;
  createdBy: string;
  sections: ReportSection[];
}

/**
 * [#REPORT-4]
 * @description 리포트 조회 성공 시의 응답 인터페이스
 * @extends {BaseResponse} 기본 API 응답 형식을 상속
 * @property {Report} data - 조회된 리포트 정보
 * @property {string} timestamp - 응답 생성 시각
 */
export interface ReportSuccessResponse {
  success: true;
  data: Report;
  timestamp: string;
}

/**
 * [#REPORT-5]
 * @description API 에러 응답 인터페이스
 * @property {boolean} success - 요청 처리 성공 여부
 * @property {Record<string, never>} data - 빈 데이터 객체
 * @property {Object} error - 에러 정보
 * @property {ErrorCode} error.code - 에러 코드
 * @property {string} error.message - 에러 메시지
 * @property {string} timestamp - 에러 발생 시각
 */
export interface ReportErrorResponse {
  success: false;
  data: Record<string, never>;
  error: {
    code: ErrorCode;
    message: string;
  };
  timestamp: string;
}

/**
 * [#REPORT-6]
 * @description 리포트 목록의 개별 항목 인터페이스
 * @property {string} reportId - 리포트의 고유 식별자
 * @property {string} title - 리포트의 제목
 * @property {string} summary - 리포트의 요약 내용
 * @property {string} createdAt - 리포트 생성 시각
 * @property {string} createdBy - 리포트 작성자 이메일
 */
export interface ReportListItem {
  reportId: string;
  title: string;
  summary: string;
  createdAt: string;
  createdBy: string;
}

/**
 * [#REPORT-7]
 * @description 페이지네이션 정보를 담는 인터페이스
 * @property {number} pageNumber - 현재 페이지 번호
 * @property {number} totalPages - 전체 페이지 수
 * @property {number} totalElements - 전체 항목 수
 * @property {number} pageSize - 페이지당 항목 수
 * @property {boolean} first - 첫 페이지 여부
 * @property {boolean} last - 마지막 페이지 여부
 */
export interface PageInfo {
  pageNumber: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  first: boolean;
  last: boolean;
}

/**
 * [#REPORT-8]
 * @description 리포트 목록 데이터를 담는 인터페이스
 * @extends {PageInfo} 페이지네이션 정보를 상속
 * @property {ReportListItem[]} content - 리포트 목록 배열
 */
export interface ReportListData extends PageInfo {
  content: ReportListItem[];
}

/**
 * [#REPORT-9]
 * @description 리포트 목록 조회 성공 시의 응답 인터페이스
 * @property {boolean} success - 요청 처리 성공 여부
 * @property {ReportListData} data - 리포트 목록 및 페이지네이션 정보
 * @property {string} timestamp - 응답 생성 시각
 */
export interface ReportListSuccessResponse {
  success: true;
  data: ReportListData;
  timestamp: string;
}

/**
 * @description API 응답에 사용되는 에러 코드
 * @typedef {string} ErrorCode
 * @enum {string}
 * @property {"INVALID_REQUEST"} - 잘못된 요청
 * @property {"UNAUTHORIZED"} - 인증되지 않은 사용자
 * @property {"REPORT_NOT_FOUND"} - 리포트를 찾을 수 없음
 * @property {"PROJECT_NOT_FOUND"} - 프로젝트를 찾을 수 없음
 * @property {"INTERNAL_ERROR"} - 서버 내부 오류
 */
export type ErrorCode = 'INVALID_REQUEST' | 'UNAUTHORIZED' | 'REPORT_NOT_FOUND' | 'PROJECT_NOT_FOUND' | 'INTERNAL_ERROR';

/**
 * [#REPORT-10]
 * @description 리포트 목록 조회 응답의 통합 타입
 * @type {ReportListSuccessResponse | ReportErrorResponse}
 */
export type ReportListResponse = ReportListSuccessResponse | ReportErrorResponse;

/**
 * [#REPORT-11]
 * @description 리포트 목록 조회 요청 파라미터 인터페이스
 * @property {number} projectId - 조회할 프로젝트의 식별자
 */
export interface ReportListParams {
  projectId: number;
}