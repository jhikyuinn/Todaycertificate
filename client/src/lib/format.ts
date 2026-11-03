/** '2026-03-01' → '3월 1일 (일)' */
export function formatDate(iso: string) {
  const [y, m, d] = iso.split('-').map(Number);
  const day = '일월화수목금토'[new Date(y, m - 1, d).getDay()];
  return `${m}월 ${d}일 (${day})`;
}

/** D-day 표시. 당일 D-DAY, 지나면 D+N */
export function dDayLabel(dDay: number) {
  if (dDay === 0) return 'D-DAY';
  return dDay > 0 ? `D-${dDay}` : `D+${-dDay}`;
}

/** 초 → '87:05' */
export function formatClock(totalSeconds: number) {
  const s = Math.max(0, Math.floor(totalSeconds));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
}
