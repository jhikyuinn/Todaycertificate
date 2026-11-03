import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors } from './ui';

export type CellState = 'none' | 'answered' | 'correct' | 'wrong';

const CELL: Record<CellState, { bg: string; fg: string; border: string }> = {
  none: { bg: colors.card, fg: colors.sub, border: colors.border },
  answered: { bg: colors.primarySoft, fg: colors.primary, border: colors.primarySoft },
  correct: { bg: colors.correctSoft, fg: colors.correct, border: colors.correctSoft },
  wrong: { bg: colors.wrongSoft, fg: colors.wrong, border: colors.wrongSoft },
};

/** 문제 번호판. 공부 모드(맞힘/틀림)와 시험 모드(답함/안 답함)가 함께 쓴다. */
export function NumberGrid({
  cells,
  current,
  onJump,
}: {
  cells: { number: number; state: CellState }[];
  current: number; // 지금 보고 있는 칸의 인덱스
  onJump: (index: number) => void;
}) {
  return (
    <View style={s.grid}>
      {cells.map((c, i) => {
        const look = CELL[c.state];
        return (
          <Pressable
            key={i}
            onPress={() => onJump(i)}
            accessibilityLabel={`${c.number}번 문제로 이동`}
            style={[
              s.cell,
              { backgroundColor: look.bg, borderColor: look.border },
              i === current && { borderColor: colors.text, borderWidth: 2 },
            ]}>
            <Text style={{ color: look.fg, fontWeight: '700', fontSize: 13 }}>{c.number}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const s = StyleSheet.create({
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  cell: { width: 38, height: 38, borderRadius: 11, borderWidth: 1, alignItems: 'center', justifyContent: 'center' },
});
