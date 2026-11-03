import { router } from 'expo-router';
import { Text, View } from 'react-native';

import { IconTile, Ionicons } from '@/components/Icon';
import { Card, colors, Empty, ErrorBox, Loading, Muted, Screen, Title } from '@/components/ui';
import { errorMessage } from '@/lib/errors';
import { useApi, useIsWide } from '@/lib/hooks';
import { CertificateItem } from '@/lib/types';

// 자격증마다 번갈아 쓰는 파스텔 색: 목록이 단조롭지 않게
const TINTS = [
  [colors.primary, colors.primarySoft],
  [colors.mint, colors.mintSoft],
  [colors.warn, colors.butterSoft],
] as const;

/**
 * 자격증 목록.
 * 넓은 화면: 3열 카드 격자 (한눈에 훑어보기). 좁은 화면: 세로 목록 (엄지로 크게 누르기).
 */
export default function CertificatesScreen() {
  const wide = useIsWide();
  const { data, error, loading, reload } = useApi<CertificateItem[]>('/api/certificates');

  return (
    <Screen>
      <Title sub="공부할 자격증을 골라 주세요">자격증</Title>
      {loading && <Loading />}
      {error && <ErrorBox message={errorMessage(error)} onRetry={reload} />}
      {data?.length === 0 && <Empty>아직 등록된 자격증이 없어요</Empty>}
      <View style={{ flexDirection: wide ? 'row' : 'column', flexWrap: 'wrap', gap: 14 }}>
        {data?.map((c, i) => {
          const [tint, soft] = TINTS[i % TINTS.length];
          return (
            <Card
              key={c.id}
              onPress={() => router.push(`/certificates/${c.id}`)}
              style={wide ? { width: '31.5%', minHeight: 150 } : { flexDirection: 'row', alignItems: 'center', gap: 14 }}>
              <IconTile name="ribbon" tint={tint} soft={soft} size={wide ? 22 : 20} />
              <View style={{ flex: wide ? undefined : 1, marginTop: wide ? 14 : 0 }}>
                <Text style={{ fontSize: 17, fontWeight: '800', color: colors.text }}>{c.name}</Text>
                {c.description ? (
                  <Muted style={{ marginTop: 4 }} >
                    {c.description}
                  </Muted>
                ) : null}
              </View>
              {!wide && <Ionicons name="chevron-forward" size={20} color={colors.sub} />}
            </Card>
          );
        })}
      </View>
    </Screen>
  );
}
