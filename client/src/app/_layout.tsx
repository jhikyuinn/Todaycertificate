import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';

import { AuthProvider, useAuth } from '@/lib/auth';

SplashScreen.preventAutoHideAsync();

/**
 * 로그인 여부로 화면 접근을 막는다 (Stack.Protected).
 * 로그인 전: login, signup만 / 로그인 후: (app) 그룹만. 주소창에 직접 쳐도 막힌다.
 * 화면 보호는 편의일 뿐이고, 실제 권한 검사는 서버가 한다.
 */
function RootNavigator() {
  const { me, isLoading } = useAuth();

  useEffect(() => {
    if (!isLoading) SplashScreen.hideAsync();
  }, [isLoading]);

  if (isLoading) return null; // 저장된 토큰 확인 전에는 아무 화면도 판단하지 않는다

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Protected guard={!me} redirectTo="/">
        <Stack.Screen name="login" />
        <Stack.Screen name="signup" />
      </Stack.Protected>
      <Stack.Protected guard={!!me} redirectTo="/login">
        <Stack.Screen name="(app)" />
      </Stack.Protected>
    </Stack>
  );
}

export default function RootLayout() {
  return (
    <AuthProvider>
      <RootNavigator />
    </AuthProvider>
  );
}
