import { PreviewRow } from './types';

// 끝의 '유형' 칸으로 객관식·주관식을 섞어 보낼 수 있다 (서버 CSV가 9칸·10칸 둘 다 받는다)
const HEADER = ['과목번호', '문제번호', '문제', '보기1', '보기2', '보기3', '보기4', '정답번호', '해설', '유형'];

// 모든 칸을 큰따옴표로 감싸면 쉼표·줄바꿈이 있어도 칸이 깨지지 않는다. 안의 " 는 "" 로.
const cell = (v: string | number | null | undefined) => `"${String(v ?? '').replace(/"/g, '""')}"`;

/**
 * 미리보기 행을 기존 업로드 API가 받는 CSV로 바꾼다. 빈 칸은 서버 CSV 검증이 행 오류로 잡는다.
 * 주관식은 보기를 비우고 '정답번호' 칸에 답 글자를 넣는다 (서버 CSV 형식이 그렇다).
 */
export function toCsv(rows: PreviewRow[]): string {
  const lines = rows.map((r) => {
    const short = r.type === 'SHORT_ANSWER';
    return [
      r.subjectNumber,
      r.number,
      r.content,
      ...[0, 1, 2, 3].map((i) => (short ? '' : r.choices[i])),
      short ? r.answerText : r.answerNo,
      r.explanation,
      short ? '주관식' : '객관식',
    ]
      .map(cell)
      .join(',');
  });
  return [HEADER.map(cell).join(','), ...lines].join('\r\n');
}
