import { router, useFocusEffect } from 'expo-router';
import { useCallback } from 'react';
import { Text, View } from 'react-native';

import { AdminGate } from '@/components/AdminGate';
import { IconTile, Ionicons } from '@/components/Icon';
import { Button, Card, colors, ErrorBox, Loading, Screen, Title } from '@/components/ui';
import { errorMessage } from '@/lib/errors';
import { useApi } from '@/lib/hooks';
import { CertificateItem } from '@/lib/types';

/** 운영자 첫 화면: 자격증 목록(수정으로 이동), 새 자격증, 기출 업로드. */
export default function AdminHome() {
  const { data, error, loading, reload } = useApi<CertificateItem[]>('/api/certificates');
  useFocusEffect(useCallback(() => void reload(), [reload]));

  return (
    <AdminGate>
      <Screen>
        <Title sub="자격증 정보와 기출 문제를 관리해요">운영자</Title>
        <View style={{ flexDirection: 'row', gap: 10, marginBottom: 20, flexWrap: 'wrap' }}>
          <Button title="+ 새 자격증" onPress={() => router.push('/admin/certificates/new')} />
          <Button title="기출 CSV 업로드" variant="secondary" onPress={() => router.push('/admin/upload')} />
        </View>
        {loading && <Loading />}
        {error && <ErrorBox message={errorMessage(error)} onRetry={reload} />}
        <View style={{ gap: 10 }}>
          {data?.map((c) => (
            <Card
              key={c.id}
              onPress={() => router.push(`/admin/certificates/${c.id}`)}
              style={{ flexDirection: 'row', alignItems: 'center', gap: 14 }}>
              <IconTile name="ribbon" tint={colors.mint} soft={colors.mintSoft} />
              <View style={{ flex: 1 }}>
                <Text style={{ fontSize: 16, fontWeight: '800', color: colors.text }}>{c.name}</Text>
                <Text style={{ color: colors.sub }} numberOfLines={1}>{c.description ?? '설명 없음'}</Text>
              </View>
              <Text style={{ color: colors.primary, fontWeight: '700' }}>수정</Text>
              <Ionicons name="chevron-forward" size={18} color={colors.sub} />
            </Card>
          ))}
        </View>
      </Screen>
    </AdminGate>
  );
}
