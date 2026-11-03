import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { api } from '@/lib/api';
import { errorMessage } from '@/lib/errors';
import { useIsWide } from '@/lib/hooks';
import { useKeyboard } from '@/lib/keyboard';
import { QuestionView, SolveResult } from '@/lib/types';

import { BottomSheet } from './BottomSheet';
import { IconTile, Ionicons } from './Icon';
import { CellState, NumberGrid } from './NumberGrid';
import { QuestionCard } from './QuestionCard';
import { RichText } from './RichText';
import { Badge, Button, Card, colors, Empty, ErrorBox, Muted, Screen, Title } from './ui';

/**
 * 공부 모드 풀이 화면. 회차 공부, 오늘의 문제, 오답노트·별표 다시 풀기가 함께 쓴다.
 * 보기를 고르면 바로 서버에 기록하고 정답·해설을 보여준다.
 */
export function StudySession({
  title,
  subtitle,
  questions: initial,
  doneHref = '/',
}: {
  title: string;
  subtitle?: string;
  questions: QuestionView[];
  doneHref?: string;
}) {
  const wide = useIsWide();
  const [questions, setQuestions] = useState(initial);
  const [index, setIndex] = useState(0);
  const [results, setResults] = useState<Record<number, SolveResult & { selectedNo: number }>>({});
  const [error, setError] = useState<string | null>(null);
  const [gridOpen, setGridOpen] = useState(false);
  const [finished, setFinished] = useState(false);

  const q = questions[index] as QuestionView | undefined;
  const result = q ? results[q.id] : undefined;
  const solvedCount = Object.keys(results).length;
  const correctCount = Object.values(results).filter((r) => r.correct).length;

  const select = async (no: number) => {
    if (!q || result) return;
    setError(null);
    try {
      const r = await api.post<SolveResult>(`/api/questions/${q.id}/solve`, { selectedNo: no });
      setResults((prev) => ({ ...prev, [q.id]: { ...r, selectedNo: no } }));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const move = (to: number) => {
    setError(null);
    if (to >= questions.length) setFinished(true);
    else setIndex(Math.max(0, to));
  };

  // 별표는 화면에 먼저 반영하고 서버에 보낸다. 실패하면 되돌린다
  const toggleBookmark = async () => {
    if (!q) return;
    const on = !q.bookmarked;
    const update = (value: boolean) =>
      setQuestions((prev) => prev.map((x) => (x.id === q.id ? { ...x, bookmarked: value } : x)));
    update(on);
    try {
      if (on) await api.put(`/api/questions/${q.id}/bookmark`);
      else await api.delete(`/api/questions/${q.id}/bookmark`);
    } catch (e) {
      update(!on);
      setError(errorMessage(e));
    }
  };

  useKeyboard((key) => {
    if (finished || !q) return;
    if (['1', '2', '3', '4'].includes(key)) select(Number(key));
    else if (key === 'ArrowRight' || (key === 'Enter' && result)) move(index + 1);
    else if (key === 'ArrowLeft') move(index - 1);
  });

  // 훅(useKeyboard)을 모두 호출한 뒤에만 일찍 return한다: 훅 호출 순서가 매번 같아야 한다
  if (!q) return <Screen><Title>{title}</Title><Empty>풀 문제가 없어요</Empty></Screen>;

  if (finished) {
    return (
      <Screen maxWidth={560}>
        <Card style={{ alignItems: 'center', gap: 12, paddingVertical: 32 }}>
          <IconTile name="sparkles" tint={colors.warn} soft={colors.butterSoft} size={30} />
          <Text style={s.doneTitle}>다 풀었어요!</Text>
          <Text style={s.doneScore}>
            {solvedCount}문제 중 <Text style={{ color: colors.correct }}>{correctCount}문제</Text> 정답
          </Text>
          <Muted>틀린 문제는 '내 학습 › 오답노트'에 모여 있어요</Muted>
          <View style={{ flexDirection: 'row', gap: 10, marginTop: 8 }}>
            <Button title="처음부터 다시 보기" variant="secondary" onPress={() => { setFinished(false); setIndex(0); }} />
            <Button title="돌아가기" onPress={() => (router.canGoBack() ? router.back() : router.replace(doneHref as any))} />
          </View>
        </Card>
      </Screen>
    );
  }

  const cells = questions.map((x) => ({
    number: x.number,
    state: (results[x.id] ? (results[x.id].correct ? 'correct' : 'wrong') : 'none') as CellState,
  }));

  const explanation = result && (
    <Card style={[s.explain, { borderColor: result.correct ? colors.correctSoft : colors.wrongSoft }]}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 8 }}>
        <Ionicons
          name={result.correct ? 'checkmark-circle' : 'close-circle'}
          size={24}
          color={result.correct ? colors.correct : colors.wrong}
        />
        <Text style={[s.verdict, { color: result.correct ? colors.correct : colors.wrong }]}>
          {result.correct ? '정답이에요!' : `아쉬워요. 정답은 ${result.answerNo}번`}
        </Text>
      </View>
      {result.explanation ? <RichText text={result.explanation} /> : <Muted>등록된 해설이 없어요</Muted>}
    </Card>
  );

  const card = (
    <QuestionCard
      question={q}
      selectedNo={result?.selectedNo ?? null}
      onSelect={select}
      revealedAnswerNo={result?.answerNo}
      onToggleBookmark={toggleBookmark}
      stats={result?.stats}
      label={`${index + 1} / ${questions.length}`}
    />
  );

  const progress = (
    <View style={s.progressTrack}>
      <View style={[s.progressFill, { width: `${((index + 1) / questions.length) * 100}%` }]} />
    </View>
  );

  if (wide) {
    return (
      <Screen maxWidth={1180}>
        <Title sub={subtitle}>{title}</Title>
        <View style={{ flexDirection: 'row', gap: 24, alignItems: 'flex-start' }}>
          <View style={{ flex: 1, gap: 16 }}>
            {progress}
            {error && <ErrorBox message={error} />}
            <Card style={{ padding: 28 }}>{card}</Card>
            {explanation}
            <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
              <Button title="← 이전" variant="secondary" onPress={() => move(index - 1)} disabled={index === 0} />
              <Button
                title={index === questions.length - 1 ? '끝내기' : '다음 →'}
                onPress={() => move(index + 1)}
                variant={result ? 'primary' : 'secondary'}
              />
            </View>
          </View>
          <Card style={{ width: 268, gap: 14 }}>
            <Text style={s.sideTitle}>문제 번호</Text>
            <View style={{ flexDirection: 'row', gap: 8 }}>
              <Badge text={`정답 ${correctCount}`} tone="correct" />
              <Badge text={`오답 ${solvedCount - correctCount}`} tone="wrong" />
            </View>
            <NumberGrid cells={cells} current={index} onJump={move} />
            <View style={s.keys}>
              <Muted>⌨ 1~4 보기 선택</Muted>
              <Muted>⌨ ← → 이전·다음, Enter 다음</Muted>
            </View>
          </Card>
        </View>
      </Screen>
    );
  }

  return (
    <Screen
      footer={
        <View style={{ flexDirection: 'row', gap: 10 }}>
          <Button title="이전" variant="secondary" onPress={() => move(index - 1)} disabled={index === 0} big style={{ flex: 1 }} />
          <Button
            title={index === questions.length - 1 ? '끝내기' : '다음'}
            onPress={() => move(index + 1)}
            variant={result ? 'primary' : 'secondary'}
            big
            style={{ flex: 2 }}
          />
        </View>
      }>
      <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 10, gap: 10 }}>
        <View style={{ flex: 1 }}>
          <Text style={s.mobileTitle} numberOfLines={1}>{title}</Text>
        </View>
        <Pressable onPress={() => setGridOpen(true)} style={s.gridButton}>
          <Ionicons name="grid" size={16} color={colors.text} />
          <Text style={{ fontWeight: '700', color: colors.text }}>번호</Text>
        </Pressable>
      </View>
      {progress}
      <View style={{ height: 14 }} />
      {error && <ErrorBox message={error} />}
      {card}
      <View style={{ height: 14 }} />
      {explanation}
      <BottomSheet visible={gridOpen} onClose={() => setGridOpen(false)} title={`정답 ${correctCount} · 오답 ${solvedCount - correctCount}`}>
        <NumberGrid
          cells={cells}
          current={index}
          onJump={(i) => {
            setGridOpen(false);
            move(i);
          }}
        />
      </BottomSheet>
    </Screen>
  );
}

const s = StyleSheet.create({
  explain: { borderWidth: 2 },
  verdict: { fontSize: 17, fontWeight: '800' },
  progressTrack: { height: 8, borderRadius: 4, backgroundColor: colors.border, overflow: 'hidden' },
  progressFill: { height: 8, borderRadius: 4, backgroundColor: colors.primary },
  sideTitle: { fontSize: 16, fontWeight: '800', color: colors.text },
  keys: { gap: 4, paddingTop: 10, borderTopWidth: 1, borderTopColor: colors.border },
  mobileTitle: { fontSize: 17, fontWeight: '800', color: colors.text },
  gridButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 12,
    backgroundColor: colors.butterSoft,
  },
  doneTitle: { fontSize: 24, fontWeight: '900', color: colors.text },
  doneScore: { fontSize: 18, fontWeight: '700', color: colors.text },
});
