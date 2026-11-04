import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { IconName, IconTile, Ionicons } from '@/components/Icon';
import { SubjectReportCard } from '@/components/SubjectReportCard';
import { Button, Card, colors, Muted, Screen, Title } from '@/components/ui';
import { useAuth } from '@/lib/auth';
import { useApi, useIsWide } from '@/lib/hooks';
import { NoteItem } from '@/lib/types';

/** 내 학습: 오답노트·별표·점수 이력으로 가는 입구, 과락 위험 리포트, (운영자에게만) 운영자 화면 입구. */
export default function MyScreen() {
  const wide = useIsWide();
  const { me, logout } = useAuth();
  const wrong = useApi<NoteItem[]>('/api/me/wrong-notes');
  const bookmarks = useApi<NoteItem[]>('/api/me/bookmarks');
  const [focusKey, setFocusKey] = useState(0);

  // 이 화면은 Stack에 남아 있으므로, 문제를 풀고 돌아올 때마다 다시 불러온다
  const { reload: reloadWrong } = wrong;
  const { reload: reloadBookmarks } = bookmarks;
  useFocusEffect(
    useCallback(() => {
      reloadWrong();
      reloadBookmarks();
      setFocusKey((k) => k + 1);
    }, [reloadWrong, reloadBookmarks]),
  );

  const tiles = (
    <View style={{ flexDirection: wide ? 'row' : 'column', gap: 12 }}>
      <Tile
        icon="close-circle"
        tint={colors.wrong}
        soft={colors.wrongSoft}
        title="오답노트"
        count={wrong.data?.length}
        onPress={() => router.push('/notes/wrong')}
      />
      <Tile
        icon="star"
        tint={colors.warn}
        soft={colors.butterSoft}
        title="별표 문제"
        count={bookmarks.data?.length}
        onPress={() => router.push('/notes/bookmarks')}
      />
    </View>
  );

  const historyCard = (
    <Card onPress={() => router.push('/history')} style={s.admin}>
      <IconTile name="trending-up" tint={colors.mint} soft={colors.mintSoft} />
      <View style={{ flex: 1 }}>
        <Text style={s.tileTitle}>점수 이력</Text>
        <Muted>시험 모드 점수 변화</Muted>
      </View>
      <Ionicons name="chevron-forward" size={20} color={colors.sub} />
    </Card>
  );

  return (
    <Screen>
      <Title sub={`${me?.loginId} · ${me?.role === 'ADMIN' ? '운영자' : '학습자'}`}>{me?.name}님의 학습</Title>
      <View style={{ gap: 16 }}>
        {tiles}
        {historyCard}
        <SubjectReportCard key={focusKey} />
        {me?.role === 'ADMIN' && (
          <Card onPress={() => router.push('/admin')} style={s.admin}>
            <IconTile name="construct" tint={colors.sub} soft={colors.border} />
            <View style={{ flex: 1 }}>
              <Text style={s.tileTitle}>운영자 화면</Text>
              <Muted>자격증 편집, 기출 CSV 업로드</Muted>
            </View>
            <Ionicons name="chevron-forward" size={20} color={colors.sub} />
          </Card>
        )}
        {!wide && <Button title="로그아웃" variant="secondary" onPress={logout} />}
      </View>
    </Screen>
  );
}

function Tile(props: { icon: IconName; tint: string; soft: string; title: string; count?: number; onPress: () => void }) {
  return (
    <Pressable
      onPress={props.onPress}
      style={({ hovered }: any) => [s.tile, { backgroundColor: props.soft }, hovered && { opacity: 0.85 }]}>
      <IconTile name={props.icon} tint={props.tint} soft={colors.card} size={22} />
      <View style={{ flex: 1 }}>
        <Text style={s.tileTitle}>{props.title}</Text>
        <Text style={[s.count, { color: props.tint }]}>{props.count ?? '-'}문제</Text>
      </View>
      <Ionicons name="chevron-forward" size={20} color={props.tint} />
    </Pressable>
  );
}

const s = StyleSheet.create({
  tile: { flex: 1, flexDirection: 'row', alignItems: 'center', gap: 14, padding: 18, borderRadius: 20 },
  tileTitle: { fontSize: 17, fontWeight: '800', color: colors.text },
  count: { fontSize: 15, fontWeight: '800', marginTop: 2 },
  admin: { flexDirection: 'row', alignItems: 'center', gap: 14 },
});
