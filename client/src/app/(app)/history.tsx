import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Ionicons } from '@/components/Icon';
import { ScoreChart } from '@/components/ScoreChart';
import { Badge, Card, colors, Empty, ErrorBox, Loading, Muted, Screen, Title } from '@/components/ui';
import { useAuth } from '@/lib/auth';
import { errorMessage } from '@/lib/errors';
import { useApi } from '@/lib/hooks';
import { CertificateDetail, CertificateItem, HistoryItem } from '@/lib/types';

/** 시험 점수 이력 (#32). 자격증을 고르면 그 자격증의 제출한 시험을 그래프와 목록으로 보여준다. */
export default function HistoryScreen() {
  const { me } = useAuth();
  const certs = useApi<CertificateItem[]>('/api/certificates');
  const [picked, setPicked] = useState<number | null>(null);
  // 고른 게 없으면 내 목표 시험 자격증, 그것도 없으면 첫 번째
  const certId = picked ?? me?.target?.certificateId ?? certs.data?.[0]?.id ?? null;
  const history = useApi<HistoryItem[]>(certId ? `/api/me/attempts?certificateId=${certId}` : null);
  const detail = useApi<CertificateDetail>(certId ? `/api/certificates/${certId}` : null); // 합격선(passAverage)

  if (certs.loading) return <Loading />;
  if (certs.error) return <Screen><ErrorBox message={errorMessage(certs.error)} onRetry={certs.reload} /></Screen>;

  const items = history.data ?? [];

  return (
    <Screen>
      <Title sub="시험 모드로 본 시험의 평균 점수예요">점수 이력</Title>
      <View style={s.pills}>
        {certs.data?.map((c) => (
          <Pressable key={c.id} onPress={() => setPicked(c.id)} style={[s.pill, certId === c.id && s.pillOn]}>
            <Text style={{ fontWeight: '700', color: certId === c.id ? '#fff' : colors.text }}>{c.name}</Text>
          </Pressable>
        ))}
      </View>

      {history.loading ? (
        <Loading />
      ) : history.error ? (
        <ErrorBox message={errorMessage(history.error)} onRetry={history.reload} />
      ) : items.length === 0 ? (
        <Empty>아직 본 시험이 없어요</Empty>
      ) : (
        <View style={{ gap: 16 }}>
          {items.length >= 2 && (
            <Card style={{ gap: 8 }}>
              <ScoreChart items={items} passLine={detail.data?.passAverage ?? 60} />
              <Muted>점선은 합격 기준({detail.data?.passAverage ?? 60}점)이에요</Muted>
            </Card>
          )}
          <View style={{ gap: 10 }}>
            {[...items].reverse().map((it) => (
              <Card key={it.attemptId} onPress={() => router.push(`/attempts/${it.attemptId}`)} style={s.row}>
                <View style={{ flex: 1 }}>
                  <Text style={s.round}>
                    {it.examYear}년 {it.examRound}회
                  </Text>
                  <Muted>{new Date(it.submittedAt).toLocaleDateString('ko-KR')} 제출</Muted>
                </View>
                <Text style={[s.score, { color: it.passed ? colors.mint : colors.wrong }]}>{it.averageScore.toFixed(1)}점</Text>
                <View>
                  {/* Badge는 alignSelf: flex-start라 View로 감싸야 줄 가운데에 온다 */}
                  <Badge text={it.passed ? '합격' : '불합격'} tone={it.passed ? 'correct' : 'wrong'} />
                </View>
                <Ionicons name="chevron-forward" size={18} color={colors.sub} />
              </Card>
            ))}
          </View>
        </View>
      )}
    </Screen>
  );
}

const s = StyleSheet.create({
  pills: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 16 },
  pill: { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 999, borderWidth: 1.5, borderColor: colors.border },
  pillOn: { backgroundColor: colors.primary, borderColor: colors.primary },
  row: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  round: { fontSize: 16, fontWeight: '800', color: colors.text },
  score: { fontSize: 17, fontWeight: '800' },
});
