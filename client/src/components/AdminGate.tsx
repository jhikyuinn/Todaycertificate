import { ReactNode } from 'react';
import { Platform, View } from 'react-native';

import { useAuth } from '@/lib/auth';
import { useIsWide } from '@/lib/hooks';

import { Ionicons } from './Icon';
import { colors, Empty, Muted } from './ui';

/**
 * 운영자 화면 공통 문지기. 화면 숨김은 편의이고, 실제 권한 검사는 서버(/api/admin/** → ADMIN)가 한다.
 * 운영자 화면은 PC 웹 기준으로 만들었다 (CSV 파일 선택이 웹 전용). 좁은 화면에서는 안내를 위에 띄운다.
 */
export function AdminGate({ children }: { children: ReactNode }) {
  const { me } = useAuth();
  const wide = useIsWide();
  if (me?.role !== 'ADMIN') return <Empty>운영자만 볼 수 있는 화면이에요</Empty>;
  return (
    <>
      {(!wide || Platform.OS !== 'web') && (
        <View style={{ flexDirection: 'row', gap: 8, padding: 12, backgroundColor: colors.butterSoft, alignItems: 'center' }}>
          <Ionicons name="desktop-outline" size={18} color={colors.warn} />
          <Muted style={{ flex: 1 }}>운영자 화면은 PC 웹에 맞춰져 있어요. CSV 업로드는 웹에서만 돼요</Muted>
        </View>
      )}
      {children}
    </>
  );
}
