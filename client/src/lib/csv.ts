import { PreviewRow } from './types';

const HEADER = ['과목번호', '문제번호', '문제', '보기1', '보기2', '보기3', '보기4', '정답번호', '해설'];

// 모든 칸을 큰따옴표로 감싸면 쉼표·줄바꿈이 있어도 칸이 깨지지 않는다. 안의 " 는 "" 로.
const cell = (v: string | number | null | undefined) => `"${String(v ?? '').replace(/"/g, '""')}"`;

/** 미리보기 행을 기존 업로드 API가 받는 9칸 CSV로 바꾼다. 빈 칸은 서버 CSV 검증이 행 오류로 잡는다. */
export function toCsv(rows: PreviewRow[]): string {
  const lines = rows.map((r) =>
    [r.subjectNumber, r.number, r.content, ...[0, 1, 2, 3].map((i) => r.choices[i]), r.answerNo, r.explanation]
      .map(cell)
      .join(','),
  );
  return [HEADER.map(cell).join(','), ...lines].join('\r\n');
}
