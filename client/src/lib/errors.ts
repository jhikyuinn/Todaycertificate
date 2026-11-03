import { ApiError } from './api';

/** 서버 오류 code를 화면 문구로. 모르는 code는 서버가 준 message를 그대로 쓴다. */
const MESSAGES: Record<string, string> = {
  NETWORK: '서버에 연결할 수 없습니다. 서버가 켜져 있는지 확인해 주세요.',
  INVALID_CREDENTIALS: '아이디 또는 비밀번호가 올바르지 않습니다.',
  DUPLICATE_LOGIN_ID: '이미 사용 중인 아이디입니다.',
  DUPLICATE_NAME: '이미 있는 자격증 이름입니다.',
  SUBJECT_IN_USE: '문제가 등록된 과목은 삭제할 수 없습니다.',
  ALREADY_SUBMITTED: '이미 제출한 시험입니다.',
  FORBIDDEN: '권한이 없습니다.',
  NOT_FOUND: '찾을 수 없습니다.',
};

export function errorMessage(e: unknown): string {
  if (e instanceof ApiError) return MESSAGES[e.code] ?? e.message;
  return '알 수 없는 오류가 발생했습니다.';
}

/** VALIDATION_FAILED의 details를 {필드명: 메시지}로. 입력칸 아래에 보여줄 때 쓴다. */
export function fieldErrors(e: unknown): Record<string, string> {
  if (!(e instanceof ApiError) || e.code !== 'VALIDATION_FAILED') return {};
  return Object.fromEntries(e.details.map((d: any) => [d.field, d.message]));
}
