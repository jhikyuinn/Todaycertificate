import { StyleSheet, Text, View } from 'react-native';

import { useApi } from '@/lib/hooks';
import { SubjectReport } from '@/lib/types';

import { Ionicons } from './Icon';
import { Badge, Card, colors, H2, Muted } from './ui';

/** 과목별 과락 위험 (#22). 문제마다 가장 최근 풀이 기준 정답률. 5문제 미만이면 판단 보류. */
export function SubjectReportCard({ compact = false }: { compact?: boolean }) {
  const { data } = useApi<SubjectReport[]>('/api/me/subject-report');
  if (!data || data.length === 0) return null;

  const risky = data.filter((r) => r.atRisk);
  const rows = compact ? risky : data;

  return (
    <Card>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
        <Ionicons name="pulse" size={20} color={risky.length ? colors.wrong : colors.mint} />
        <H2>과목별 과락 위험</H2>
      </View>
      {compact && risky.length === 0 && <Muted>지금은 과락 위험 과목이 없어요 👍</Muted>}
      <View style={{ gap: 12 }}>
        {rows.map((r) => (
          <View key={r.subjectNumber} style={{ gap: 6 }}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
              <Text style={s.name} numberOfLines={1}>
                {r.subjectNumber}. {r.subjectName}
              </Text>
              {r.atRisk ? (
                <Badge text="과락 위험" tone="wrong" />
              ) : r.solvedCount < 5 ? (
                <Badge text="더 풀어 보기" tone="warn" />
              ) : (
                <Badge text="안정" tone="correct" />
              )}
              <Text style={s.rate}>{r.solvedCount ? `${r.correctRate}%` : '-'}</Text>
            </View>
            <View style={s.track}>
              <View
                style={[s.fill, { width: `${r.correctRate}%`, backgroundColor: r.atRisk ? colors.wrong : colors.mint }]}
              />
            </View>
            <Muted style={{ fontSize: 12 }}>{r.solvedCount}문제 풀이</Muted>
          </View>
        ))}
      </View>
    </Card>
  );
}

const s = StyleSheet.create({
  name: { flex: 1, color: colors.text, fontWeight: '600' },
  rate: { fontWeight: '800', color: colors.text, width: 44, textAlign: 'right' },
  track: { height: 10, borderRadius: 5, backgroundColor: colors.border, overflow: 'hidden' },
  fill: { height: 10, borderRadius: 5 },
});
