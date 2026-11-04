import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { IconTile, Ionicons } from '@/components/Icon';
import { RichText } from '@/components/RichText';
import { Badge, Button, Card, colors, ErrorBox, H2, Loading, Muted, Screen } from '@/components/ui';
import { errorMessage } from '@/lib/errors';
import { formatAccepted } from '@/lib/format';
import { useApi, useIsWide } from '@/lib/hooks';
import { AttemptResult, QuestionResult } from '@/lib/types';

const FAIL_SCORE = 40; // 막대 그래프의 과락선 표시용. 판정 자체는 서버가 한다

/**
 * 시험 결과. 제출 응답을 넘겨받지 않고 GET /api/attempts/{id}로 불러온다 → 새로고침해도 유지된다.
 * 넓은 화면: 왼쪽 점수, 오른쪽 문제별 해설. 좁은 화면: 점수 → 문제 순서로 쌓는다.
 */
export default function AttemptResultScreen() {
  const { attemptId } = useLocalSearchParams<{ attemptId: string }>();
  const wide = useIsWide();
  const { data: r, error, loading, reload } = useApi<AttemptResult>(`/api/attempts/${attemptId}`);
  const [wrongOnly, setWrongOnly] = useState(false);

  if (loading) return <Loading />;
  if (error || !r) return <Screen><ErrorBox message={errorMessage(error)} onRetry={reload} /></Screen>;

  const correct = r.questions.filter((q) => q.correct).length;
  const shown = wrongOnly ? r.questions.filter((q) => !q.correct) : r.questions;

  const score = (
    <View style={{ gap: 16, width: wide ? 360 : '100%' }}>
      <Card style={[s.hero, { backgroundColor: r.passed ? colors.mintSoft : colors.wrongSoft, borderColor: 'transparent' }]}>
        <IconTile
          name={r.passed ? 'trophy' : 'refresh'}
          tint={r.passed ? colors.mint : colors.wrong}
          soft={colors.card}
          size={28}
        />
        <Text style={[s.verdict, { color: r.passed ? colors.mint : colors.wrong }]}>{r.passed ? '합격!' : '불합격'}</Text>
        <Text style={s.avg}>평균 {r.averageScore.toFixed(1)}점</Text>
        <Muted>
          {r.questions.length}문제 중 {correct}문제 정답
        </Muted>
      </Card>

      <Card>
        <H2>과목별 점수</H2>
        <View style={{ gap: 14 }}>
          {r.subjects.map((sub) => (
            <View key={sub.number} style={{ gap: 6 }}>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                <Text style={{ flex: 1, color: colors.text, fontWeight: '600' }} numberOfLines={1}>
                  {sub.number}. {sub.name}
                </Text>
                {sub.failed && <Badge text="과락" tone="wrong" />}
                <Text style={{ fontWeight: '800', color: sub.failed ? colors.wrong : colors.text }}>{sub.score}점</Text>
              </View>
              <View style={s.barTrack}>
                <View
                  style={[s.barFill, { width: `${sub.score}%`, backgroundColor: sub.failed ? colors.wrong : colors.mint }]}
                />
                <View style={[s.failLine, { left: `${FAIL_SCORE}%` }]} />
              </View>
            </View>
          ))}
        </View>
        <Muted style={{ marginTop: 12 }}>세로선은 과락 기준(40점)이에요</Muted>
      </Card>

      <View style={{ flexDirection: 'row', gap: 10 }}>
        <Button title="오답노트 보기" variant="secondary" onPress={() => router.push('/notes/wrong')} style={{ flex: 1 }} />
        <Button title="홈으로" onPress={() => router.replace('/')} style={{ flex: 1 }} />
      </View>
    </View>
  );

  const list = (
    <View style={{ flex: wide ? 1 : undefined, width: wide ? undefined : '100%', gap: 10 }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
        <H2>문제별 해설</H2>
        <Pressable onPress={() => setWrongOnly(!wrongOnly)} style={[s.toggle, wrongOnly && { backgroundColor: colors.wrongSoft }]}>
          <Ionicons name={wrongOnly ? 'checkbox' : 'square-outline'} size={16} color={wrongOnly ? colors.wrong : colors.sub} />
          <Text style={{ color: colors.text, fontWeight: '600' }}>틀린 문제만</Text>
        </Pressable>
      </View>
      {shown.map((q) => (
        <ResultRow key={q.questionId} q={q} />
      ))}
    </View>
  );

  return (
    <Screen maxWidth={1180}>
      {wide ? (
        <View style={{ flexDirection: 'row', gap: 24, alignItems: 'flex-start' }}>
          {score}
          {list}
        </View>
      ) : (
        <View style={{ gap: 24 }}>
          {score}
          {list}
        </View>
      )}
    </Screen>
  );
}

/** 한 줄 요약을 누르면 해설이 펼쳐진다. */
function ResultRow({ q }: { q: QuestionResult }) {
  const [open, setOpen] = useState(!q.correct);
  return (
    <Card style={{ paddingVertical: 12 }}>
      <Pressable onPress={() => setOpen(!open)} style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
        <Ionicons name={q.correct ? 'checkmark-circle' : 'close-circle'} size={22} color={q.correct ? colors.correct : colors.wrong} />
        <Text style={{ fontWeight: '800', color: colors.text, width: 40 }}>{q.number}번</Text>
        <Text style={{ flex: 1, color: colors.sub }}>
          {q.type === 'SHORT_ANSWER'
            ? `내 답 ${q.answerText ?? '안 풂'} · 정답 ${formatAccepted(q.correctAnswerText ?? '')}`
            : `내 답 ${q.selectedNo ?? '안 풂'} · 정답 ${q.answerNo}`}
        </Text>
        <Ionicons name={open ? 'chevron-up' : 'chevron-down'} size={18} color={colors.sub} />
      </Pressable>
      {open && (
        <View style={{ marginTop: 10, gap: 6 }}>
          {q.explanation ? <RichText text={q.explanation} /> : <Muted>등록된 해설이 없어요</Muted>}
          {q.stats && (
            <Muted>
              전체 정답률 {q.stats.correctRate}%
              {q.stats.mostChosenWrongNo ? ` · 많이 고른 오답 ${q.stats.mostChosenWrongNo}번` : ''}
            </Muted>
          )}
        </View>
      )}
    </Card>
  );
}

const s = StyleSheet.create({
  hero: { alignItems: 'center', gap: 6, paddingVertical: 28 },
  verdict: { fontSize: 32, fontWeight: '900', marginTop: 6 },
  avg: { fontSize: 20, fontWeight: '800', color: colors.text },
  barTrack: { height: 12, borderRadius: 6, backgroundColor: colors.border, overflow: 'hidden' },
  barFill: { height: 12, borderRadius: 6 },
  failLine: { position: 'absolute', top: 0, bottom: 0, width: 2, backgroundColor: colors.text, opacity: 0.4 },
  toggle: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: 10, paddingVertical: 6, borderRadius: 10 },
});
