import { StudySession } from '@/components/StudySession';
import { ErrorBox, Loading, Screen } from '@/components/ui';
import { errorMessage } from '@/lib/errors';
import { useApi } from '@/lib/hooks';
import { QuestionView } from '@/lib/types';

/** 오늘의 문제 (#20): 서버가 고른 최대 20문제를 공부 모드로 푼다. */
export default function DailyScreen() {
  const { data, error, loading, reload } = useApi<QuestionView[]>('/api/me/daily-problems');

  if (loading) return <Loading />;
  if (error || !data) return <Screen><ErrorBox message={errorMessage(error)} onRetry={reload} /></Screen>;
  return <StudySession title="오늘의 문제" subtitle="오답 → 약한 과목 → 안 푼 문제 순서로 골랐어요" questions={data} />;
}
