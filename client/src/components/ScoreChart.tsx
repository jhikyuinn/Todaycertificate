import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { useIsWide } from '@/lib/hooks';
import { HistoryItem } from '@/lib/types';

import { colors } from './ui';

const BAR = 36; // 막대 하나의 폭 + 간격 계산에 쓴다
const GAP = 18;

/**
 * 점수 이력 막대 그래프. 그래프 라이브러리 없이 View 높이로 그린다.
 * 막대 높이 = 평균 점수, 합격은 mint / 불합격은 wrong, passLine 위치에 점선. 많으면 가로로 스크롤한다.
 */
export function ScoreChart({ items, passLine }: { items: HistoryItem[]; passLine: number }) {
  const height = useIsWide() ? 220 : 160;

  return (
    // flexGrow: 막대가 적어도 합격선이 카드 폭 끝까지 이어지게 한다
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ flexGrow: 1 }}>
      <View style={{ flexGrow: 1, paddingTop: 22, paddingHorizontal: 4 }}>
        <View style={{ height, flexDirection: 'row', alignItems: 'flex-end', gap: GAP }}>
          <View style={[s.passLine, { bottom: (passLine / 100) * height }]} />
          {items.map((it) => (
            <View key={it.attemptId} style={{ width: BAR, alignItems: 'center' }}>
              <Text style={[s.score, { color: it.passed ? colors.mint : colors.wrong }]}>{Math.round(it.averageScore)}</Text>
              <View
                accessibilityLabel={`${it.examYear}년 ${it.examRound}회 ${it.averageScore}점 ${it.passed ? '합격' : '불합격'}`}
                style={[s.bar, { height: (it.averageScore / 100) * height, backgroundColor: it.passed ? colors.mint : colors.wrong }]}
              />
            </View>
          ))}
        </View>
        <View style={[s.axis, { gap: GAP }]}>
          {items.map((it) => (
            <Text key={it.attemptId} style={s.label} numberOfLines={1}>
              {it.examYear}-{it.examRound}
            </Text>
          ))}
        </View>
      </View>
    </ScrollView>
  );
}

const s = StyleSheet.create({
  passLine: {
    position: 'absolute',
    left: 0,
    right: 0,
    borderTopWidth: 1.5,
    borderStyle: 'dashed',
    borderColor: colors.sub,
  },
  bar: { width: BAR, borderTopLeftRadius: 8, borderTopRightRadius: 8, minHeight: 2 },
  score: { fontSize: 12, fontWeight: '800', marginBottom: 4 },
  axis: { flexDirection: 'row', borderTopWidth: 1, borderTopColor: colors.border, paddingTop: 6 },
  label: { width: BAR, textAlign: 'center', fontSize: 11, color: colors.sub },
});
