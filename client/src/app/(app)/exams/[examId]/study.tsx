import { useLocalSearchParams } from 'expo-router';

import { StudySession } from '@/components/StudySession';
import { ErrorBox, Loading, Screen } from '@/components/ui';
import { errorMessage } from '@/lib/errors';
import { useApi } from '@/lib/hooks';
import { QuestionView } from '@/lib/types';

/** 회차 공부 모드: 회차의 문제를 번호순으로 하나씩 푼다. */
export default function StudyScreen() {
  const { examId } = useLocalSearchParams<{ examId: string }>();
  const { data, error, loading, reload } = useApi<QuestionView[]>(`/api/exams/${examId}/questions`);

  if (loading) return <Loading />;
  if (error || !data) return <Screen><ErrorBox message={errorMessage(error)} onRetry={reload} /></Screen>;
  return <StudySession title="공부 모드" subtitle="보기를 고르면 바로 정답과 해설이 나와요" questions={data} />;
}
