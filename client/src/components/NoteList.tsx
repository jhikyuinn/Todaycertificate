import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { useApi, useIsWide } from '@/lib/hooks';
import { errorMessage } from '@/lib/errors';
import { NoteItem } from '@/lib/types';

import { IconName, IconTile } from './Icon';
import { StudySession } from './StudySession';
import { Badge, Button, Card, colors, Empty, ErrorBox, Loading, Muted, Screen, Title } from './ui';

/**
 * 오답노트·별표 목록 공통 화면. "다시 풀기"를 누르면 같은 화면에서 공부 모드로 바뀌고,
 * 다 풀면 목록을 다시 불러온다 (다시 맞힌 문제는 오답노트에서 빠진다).
 */
export function NoteList({
  path,
  title,
  sub,
  emptyText,
  icon,
  tint,
  soft,
}: {
  path: string;
  title: string;
  sub: string;
  emptyText: string;
  icon: IconName;
  tint: string;
  soft: string;
}) {
  const wide = useIsWide();
  const { data, error, loading, reload } = useApi<NoteItem[]>(path);
  const [solving, setSolving] = useState<NoteItem[] | null>(null);

  if (solving) {
    return (
      <StudySession
        title={`${title} 다시 풀기`}
        questions={solving.map((n) => n.question)}
        onDone={() => {
          setSolving(null);
          reload();
        }}
      />
    );
  }

  const items = data ?? [];
  const solveAll = () => setSolving(items);

  return (
    <Screen
      footer={!wide && items.length > 0 ? <Button title={`${items.length}문제 전부 다시 풀기`} onPress={solveAll} big /> : undefined}>
      <View style={{ flexDirection: 'row', alignItems: 'flex-start', gap: 12 }}>
        <IconTile name={icon} tint={tint} soft={soft} size={22} />
        <View style={{ flex: 1 }}>
          <Title sub={sub}>{title}</Title>
        </View>
        {wide && items.length > 0 && <Button title={`${items.length}문제 전부 다시 풀기`} onPress={solveAll} />}
      </View>
      {loading && <Loading />}
      {error && <ErrorBox message={errorMessage(error)} onRetry={reload} />}
      {!loading && items.length === 0 && !error && <Empty>{emptyText}</Empty>}
      <View style={{ gap: 10 }}>
        {items.map((n) => (
          <Card key={n.question.id} onPress={() => setSolving([n, ...items.filter((x) => x !== n)])} style={s.row}>
            <View style={{ flex: 1, gap: 6 }}>
              <View style={{ flexDirection: 'row', gap: 6, flexWrap: 'wrap' }}>
                <Badge text={n.certificateName} />
                <Badge text={`${n.examYear}년 ${n.examRound}회 ${n.question.number}번`} tone="warn" />
              </View>
              {/* 코드 블록은 목록에서 빼고 첫 줄만 보여준다 */}
              <Text style={s.preview} numberOfLines={wide ? 1 : 2}>
                {n.question.content.split('```')[0].trim() || '코드 문제'}
              </Text>
              <Muted>
                {n.question.subjectNumber}과목 · {n.question.subjectName}
              </Muted>
            </View>
            <Text style={s.go}>풀기 ›</Text>
          </Card>
        ))}
      </View>
    </Screen>
  );
}

const s = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  preview: { fontSize: 16, color: colors.text, fontWeight: '600' },
  go: { color: colors.primary, fontWeight: '800' },
});
