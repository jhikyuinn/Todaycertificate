import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useIsWide } from '@/lib/hooks';
import { QuestionView, Stats } from '@/lib/types';

import { RichText } from './RichText';
import { Badge, colors } from './ui';

type Props = {
  question: QuestionView;
  selectedNo: number | null;
  onSelect?: (no: number) => void; // 없으면 보기 선택 불가 (결과 보기 등)
  revealedAnswerNo?: number | null; // 정답 공개 후: 정답은 초록, 고른 오답은 빨강
  onToggleBookmark?: () => void;
  stats?: Stats | null;
  label?: string; // "12 / 100" 같은 위치 표시
};

const CIRCLED = ['①', '②', '③', '④'];

/**
 * 문제 한 개. 공부 모드, 시험 모드, 오답노트, 오늘의 문제가 함께 쓴다.
 * 넓은 화면에서는 보기 옆에 키보드 숫자를 표시한다 (1~4로 선택).
 */
export function QuestionCard({ question, selectedNo, onSelect, revealedAnswerNo, onToggleBookmark, stats, label }: Props) {
  const wide = useIsWide();
  const revealed = revealedAnswerNo != null;

  return (
    <View style={{ gap: 16 }}>
      <View style={s.head}>
        <Badge text={`${question.subjectNumber}과목 · ${question.subjectName}`} />
        {label && <Text style={s.label}>{label}</Text>}
        {onToggleBookmark && (
          <Pressable
            onPress={onToggleBookmark}
            hitSlop={10}
            accessibilityLabel={question.bookmarked ? '별표 해제' : '별표'}
            style={{ marginLeft: 'auto' }}>
            <Text style={[s.star, question.bookmarked && { color: colors.butter }]}>{question.bookmarked ? '★' : '☆'}</Text>
          </Pressable>
        )}
      </View>

      <View style={{ flexDirection: 'row', gap: 8 }}>
        <Text style={s.number}>{question.number}.</Text>
        <View style={{ flex: 1 }}>
          <RichText text={question.content} style={{ fontSize: wide ? 17 : 16 }} />
        </View>
      </View>

      <View style={{ gap: wide ? 8 : 10 }}>
        {question.choices.map((choice, i) => {
          const no = i + 1;
          const isAnswer = revealed && no === revealedAnswerNo;
          const isWrongPick = revealed && no === selectedNo && no !== revealedAnswerNo;
          const isSelected = !revealed && no === selectedNo;
          return (
            <Pressable
              key={no}
              disabled={!onSelect || revealed}
              onPress={() => onSelect?.(no)}
              accessibilityRole="radio"
              accessibilityState={{ selected: no === selectedNo }}
              style={({ hovered }: any) => [
                s.choice,
                { paddingVertical: wide ? 12 : 16 },
                hovered && !revealed && onSelect && { borderColor: colors.primary },
                isSelected && { borderColor: colors.primary, backgroundColor: colors.primarySoft },
                isAnswer && { borderColor: colors.correct, backgroundColor: colors.correctSoft },
                isWrongPick && { borderColor: colors.wrong, backgroundColor: colors.wrongSoft },
              ]}>
              <Text style={[s.circled, isAnswer && { color: colors.correct }, isWrongPick && { color: colors.wrong }]}>
                {CIRCLED[i]}
              </Text>
              <View style={{ flex: 1 }}>
                <RichText text={choice} />
              </View>
              {wide && !revealed && onSelect && <Text style={s.key}>{no}</Text>}
              {stats && revealed && stats.mostChosenWrongNo === no && <Badge text="많이 고른 오답" tone="warn" />}
            </Pressable>
          );
        })}
      </View>

      {revealed && stats && (
        <Text style={s.stats}>
          전체 정답률 {stats.correctRate}% · {stats.solvedCount}명 풀이
        </Text>
      )}
    </View>
  );
}

const s = StyleSheet.create({
  head: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  label: { color: colors.sub, fontSize: 14 },
  star: { fontSize: 24, color: colors.sub },
  number: { fontSize: 17, fontWeight: '800', color: colors.text, lineHeight: 25 },
  choice: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingHorizontal: 14,
    borderWidth: 1.5,
    borderColor: colors.border,
    borderRadius: 16,
    backgroundColor: colors.card,
  },
  circled: { fontSize: 18, color: colors.sub, width: 22 },
  key: {
    fontSize: 12,
    color: colors.sub,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 4,
    paddingHorizontal: 6,
    paddingVertical: 1,
  },
  stats: { color: colors.sub, fontSize: 14 },
});
