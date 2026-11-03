import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { IconTile, Ionicons } from '@/components/Icon';
import { SubjectReportCard } from '@/components/SubjectReportCard';
import { Button, Card, colors, Muted, Screen, Title } from '@/components/ui';
import { useAuth } from '@/lib/auth';
import { dDayLabel, formatDate } from '@/lib/format';
import { useApi, useIsWide } from '@/lib/hooks';
import { QuestionView } from '@/lib/types';

/**
 * 홈: 앱의 핵심 "시험일까지, 오늘 풀 문제"를 한 화면에.
 * D-day(#21) · 오늘의 문제(#20) · 과락 위험 요약(#22).
 * 넓은 화면은 D-day와 오늘의 문제를 나란히, 좁은 화면은 세로로 쌓고 시작 버튼을 크게.
 */
export default function HomeScreen() {
  const wide = useIsWide();
  const { me, refresh } = useAuth();
  const daily = useApi<QuestionView[]>('/api/me/daily-problems');
  const [focusKey, setFocusKey] = useState(0);

  // 다른 화면에서 문제를 풀거나 목표를 바꾸고 돌아오면 다시 불러온다
  const { reload } = daily;
  useFocusEffect(
    useCallback(() => {
      reload();
      refresh();
      setFocusKey((k) => k + 1);
    }, [reload, refresh]),
  );

  const t = me?.target;
  const count = daily.data?.length ?? 0;

  const dday = (
    <Card style={[s.big, wide && { flex: 1 }, { backgroundColor: colors.primarySoft, borderColor: 'transparent' }]}>
      <View style={s.cardHead}>
        <IconTile name="calendar" tint={colors.primary} soft={colors.card} />
        <Text style={s.cardLabel}>목표 시험</Text>
      </View>
      {t ? (
        <>
          <Text style={[s.dday, { fontSize: wide ? 56 : 44 }]}>{dDayLabel(t.dDay)}</Text>
          <Text style={s.cardTitle}>{t.certificateName}</Text>
          <Muted>
            {t.year}년 {t.round}회 · {formatDate(t.examDate)}
          </Muted>
        </>
      ) : (
        <>
          <Text style={[s.cardTitle, { marginTop: 14 }]}>아직 목표 시험이 없어요</Text>
          <Muted>자격증 상세의 '목표로 설정'을 누르면 D-day를 세어 드려요</Muted>
          <Button title="목표 시험 정하기" variant="secondary" onPress={() => router.push('/certificates')} style={{ marginTop: 14, alignSelf: 'flex-start' }} />
        </>
      )}
    </Card>
  );

  const today = (
    <Card style={[s.big, wide && { flex: 1 }, { backgroundColor: colors.mintSoft, borderColor: 'transparent' }]}>
      <View style={s.cardHead}>
        <IconTile name="sparkles" tint={colors.mint} soft={colors.card} />
        <Text style={s.cardLabel}>오늘의 문제</Text>
      </View>
      {count > 0 ? (
        <>
          <Text style={[s.dday, { fontSize: wide ? 56 : 44, color: colors.mint }]}>{count}문제</Text>
          <Muted>틀렸던 문제와 약한 과목부터 골랐어요</Muted>
          <Button title="오늘의 문제 시작" onPress={() => router.push('/daily')} big style={{ marginTop: 16 }} />
        </>
      ) : (
        <>
          <Text style={[s.cardTitle, { marginTop: 14 }]}>아직 고를 문제가 없어요</Text>
          <Muted>목표 시험을 정하거나 아무 회차나 한 문제 풀면 오늘의 문제를 골라 드려요</Muted>
          <Button title="기출 풀러 가기" variant="secondary" onPress={() => router.push('/certificates')} style={{ marginTop: 14, alignSelf: 'flex-start' }} />
        </>
      )}
    </Card>
  );

  return (
    <Screen maxWidth={1080}>
      <Title sub="시험일까지, 오늘 풀 문제를 골라 드려요">
        {me?.name}님, 오늘도 한 걸음 <Ionicons name="footsteps" size={wide ? 26 : 20} color={colors.primary} />
      </Title>
      <View style={{ gap: 16 }}>
        <View style={{ flexDirection: wide ? 'row' : 'column', gap: 16 }}>
          {dday}
          {today}
        </View>
        <SubjectReportCard key={focusKey} compact />
      </View>
    </Screen>
  );
}

const s = StyleSheet.create({
  big: { padding: 24, borderRadius: 24 },
  cardHead: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  cardLabel: { fontSize: 15, fontWeight: '800', color: colors.text },
  dday: { fontWeight: '900', color: colors.primary, marginTop: 12, letterSpacing: -1 },
  cardTitle: { fontSize: 18, fontWeight: '800', color: colors.text, marginBottom: 4 },
});
