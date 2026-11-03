import { router, useLocalSearchParams } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { BottomSheet } from '@/components/BottomSheet';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { Ionicons } from '@/components/Icon';
import { NumberGrid } from '@/components/NumberGrid';
import { QuestionCard } from '@/components/QuestionCard';
import { Badge, Button, Card, colors, ErrorBox, Loading, Muted, Screen } from '@/components/ui';
import { api } from '@/lib/api';
import { errorMessage } from '@/lib/errors';
import { formatClock } from '@/lib/format';
import { useIsWide } from '@/lib/hooks';
import { useKeyboard } from '@/lib/keyboard';
import { StartResult } from '@/lib/types';

/**
 * 시험 모드. 시작하면 응시가 만들어지고, 제한시간이 끝나면 자동 제출한다.
 * 답은 제출 전까지 화면에만 있다 (새로고침하면 새 응시. 이어 풀기는 범위 밖).
 */
export default function ExamScreen() {
  const { examId } = useLocalSearchParams<{ examId: string }>();
  const wide = useIsWide();
  const [attempt, setAttempt] = useState<StartResult | null>(null);
  const [deadline, setDeadline] = useState(0);
  const [remaining, setRemaining] = useState(0);
  const [answers, setAnswers] = useState<Record<number, number>>({});
  const [index, setIndex] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [gridOpen, setGridOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const started = useRef(false);
  const submitted = useRef(false);

  // 개발 모드(StrictMode)는 effect를 두 번 실행한다. 응시가 두 개 생기지 않게 ref로 막는다
  useEffect(() => {
    if (started.current) return;
    started.current = true;
    api
      .post<StartResult>(`/api/exams/${examId}/attempts`)
      .then((res) => {
        // 마감 시각은 응답을 받은 순간의 내 시계 기준: 서버와 시계가 어긋나도 제한시간만큼 정확히 흐른다
        setDeadline(Date.now() + res.timeLimitMinutes * 60_000);
        setRemaining(res.timeLimitMinutes * 60);
        setAttempt(res);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [examId]);

  const submit = useCallback(async () => {
    if (!attempt || submitted.current) return;
    submitted.current = true;
    setSubmitting(true);
    try {
      await api.post(`/api/attempts/${attempt.attemptId}/submit`, {
        answers: Object.entries(answers).map(([questionId, selectedNo]) => ({ questionId: Number(questionId), selectedNo })),
      });
      router.replace(`/attempts/${attempt.attemptId}`);
    } catch (e) {
      submitted.current = false;
      setSubmitting(false);
      setConfirmOpen(false);
      setError(errorMessage(e));
    }
  }, [attempt, answers]);

  // 1초마다 "마감 - 지금"을 다시 계산한다. 탭이 백그라운드에서 멈췄다 돌아와도 시간이 맞다
  useEffect(() => {
    if (!deadline) return;
    const timer = setInterval(() => {
      const left = (deadline - Date.now()) / 1000;
      setRemaining(left);
      if (left <= 0) {
        clearInterval(timer);
        submit(); // 시간이 끝나면 확인 없이 자동 제출
      }
    }, 1000);
    return () => clearInterval(timer);
  }, [deadline, submit]);

  const questions = attempt?.questions ?? [];
  const q = questions[index];
  const move = (to: number) => setIndex(Math.min(Math.max(0, to), questions.length - 1));
  const select = (no: number) => q && setAnswers((prev) => ({ ...prev, [q.id]: no }));

  useKeyboard((key) => {
    if (!q || confirmOpen) return;
    if (['1', '2', '3', '4'].includes(key)) select(Number(key));
    else if (key === 'ArrowRight' || key === 'Enter') move(index + 1);
    else if (key === 'ArrowLeft') move(index - 1);
  });

  if (error && !attempt) return <Screen><ErrorBox message={error} /></Screen>;
  if (!attempt || !q) return <Loading />;

  const answeredCount = Object.keys(answers).length;
  const unanswered = questions.length - answeredCount;
  const urgent = remaining < 5 * 60; // 5분 남으면 빨간색
  const cells = questions.map((x) => ({ number: x.number, state: answers[x.id] ? ('answered' as const) : ('none' as const) }));
  const isLast = index === questions.length - 1;

  const timer = (
    <View style={[s.timer, urgent && { backgroundColor: colors.wrongSoft }]}>
      <Ionicons name="timer-outline" size={wide ? 22 : 18} color={urgent ? colors.wrong : colors.mint} />
      <Text style={[s.timerText, { fontSize: wide ? 26 : 18 }, urgent && { color: colors.wrong }]}>{formatClock(remaining)}</Text>
    </View>
  );

  const card = (
    <QuestionCard question={q} selectedNo={answers[q.id] ?? null} onSelect={select} label={`${index + 1} / ${questions.length}`} />
  );

  const confirm = (
    <ConfirmDialog
      visible={confirmOpen}
      title="제출할까요?"
      message={unanswered > 0 ? `안 푼 문제가 ${unanswered}개 있어요. 안 푼 문제는 오답으로 채점돼요.` : '모든 문제에 답했어요.'}
      confirmText="제출하기"
      onConfirm={submit}
      onCancel={() => setConfirmOpen(false)}
      loading={submitting}
    />
  );

  if (wide) {
    return (
      <Screen maxWidth={1180}>
        <View style={{ flexDirection: 'row', gap: 24, alignItems: 'flex-start' }}>
          <View style={{ flex: 1, gap: 16 }}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
              <Text style={s.title}>시험 모드</Text>
              <Badge text={`${attempt.timeLimitMinutes}분`} tone="warn" />
            </View>
            {error && <ErrorBox message={error} />}
            <Card style={{ padding: 28 }}>{card}</Card>
            <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
              <Button title="← 이전" variant="secondary" onPress={() => move(index - 1)} disabled={index === 0} />
              {isLast ? (
                <Button title="제출하기" onPress={() => setConfirmOpen(true)} />
              ) : (
                <Button title="다음 →" variant="secondary" onPress={() => move(index + 1)} />
              )}
            </View>
          </View>
          <Card style={{ width: 268, gap: 14 }}>
            {timer}
            <Text style={s.sideTitle}>
              답한 문제 {answeredCount} / {questions.length}
            </Text>
            <NumberGrid cells={cells} current={index} onJump={move} />
            <Button title="제출하기" onPress={() => setConfirmOpen(true)} />
            <View style={s.keys}>
              <Muted>⌨ 1~4 보기 선택 (다시 누르면 바뀜)</Muted>
              <Muted>⌨ ← → 이전·다음</Muted>
            </View>
          </Card>
        </View>
        {confirm}
      </Screen>
    );
  }

  return (
    <Screen
      footer={
        <View style={{ flexDirection: 'row', gap: 10 }}>
          <Button title="이전" variant="secondary" onPress={() => move(index - 1)} disabled={index === 0} big style={{ flex: 1 }} />
          {isLast ? (
            <Button title="제출하기" onPress={() => setConfirmOpen(true)} big style={{ flex: 2 }} />
          ) : (
            <Button title="다음" onPress={() => move(index + 1)} big style={{ flex: 2 }} />
          )}
        </View>
      }>
      <View style={s.mobileBar}>
        {timer}
        <Text style={{ color: colors.sub, fontWeight: '600' }}>
          답 {answeredCount}/{questions.length}
        </Text>
        <Pressable onPress={() => setGridOpen(true)} style={s.gridButton}>
          <Ionicons name="grid" size={16} color={colors.text} />
          <Text style={{ fontWeight: '700', color: colors.text }}>번호</Text>
        </Pressable>
      </View>
      {error && <ErrorBox message={error} />}
      {card}
      <BottomSheet visible={gridOpen} onClose={() => setGridOpen(false)} title={`답한 문제 ${answeredCount} / ${questions.length}`}>
        <NumberGrid
          cells={cells}
          current={index}
          onJump={(i) => {
            setGridOpen(false);
            move(i);
          }}
        />
        <Button
          title="제출하기"
          big
          style={{ marginTop: 16 }}
          onPress={() => {
            setGridOpen(false);
            setConfirmOpen(true);
          }}
        />
      </BottomSheet>
      {confirm}
    </Screen>
  );
}

const s = StyleSheet.create({
  title: { fontSize: 26, fontWeight: '800', color: colors.text },
  timer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 14,
    backgroundColor: colors.mintSoft,
    alignSelf: 'flex-start',
  },
  timerText: { fontWeight: '800', color: colors.text, fontVariant: ['tabular-nums'] },
  sideTitle: { fontSize: 15, fontWeight: '700', color: colors.text },
  keys: { gap: 4, paddingTop: 10, borderTopWidth: 1, borderTopColor: colors.border },
  mobileBar: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 14, justifyContent: 'space-between' },
  gridButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 12,
    backgroundColor: colors.butterSoft,
  },
});
