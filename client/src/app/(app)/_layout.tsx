import { Stack } from 'expo-router';

import { AppShell } from '@/components/AppShell';

/** 로그인 후 화면들. 틀(사이드바/하단 탭)은 AppShell이 그리고, 안쪽은 Stack으로 쌓아 뒤로가기가 된다. */
export default function AppLayout() {
  return (
    <AppShell>
      <Stack screenOptions={{ headerShown: false, animation: 'none' }} />
    </AppShell>
  );
}
