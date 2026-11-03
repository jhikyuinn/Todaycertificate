import AsyncStorage from '@react-native-async-storage/async-storage';
import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from 'react';

import { api, setOnUnauthorized, TOKEN_KEY } from './api';
import { Me, Member } from './types';

type AuthState = {
  me: Me | null;
  isLoading: boolean; // 저장된 토큰으로 내 정보를 확인하는 중
  login: (loginId: string, password: string) => Promise<void>;
  signup: (loginId: string, name: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>; // D-day 목표 변경 등 내 정보가 바뀐 뒤 다시 불러오기
};

const AuthContext = createContext<AuthState | null>(null);

/**
 * 토큰은 AsyncStorage(웹에서는 localStorage)에 둔다. 새로고침해도 로그인이 유지된다.
 * 트레이드오프: 웹에서는 XSS가 생기면 토큰을 읽힐 수 있다. HttpOnly 쿠키가 더 안전하지만 서버 세션·CSRF 처리가 늘어난다.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      setMe(await api.get<Me>('/api/me'));
    } catch {
      setMe(null);
    }
  }, []);

  useEffect(() => {
    setOnUnauthorized(() => setMe(null));
    (async () => {
      if (await AsyncStorage.getItem(TOKEN_KEY)) await refresh();
      setIsLoading(false);
    })();
  }, [refresh]);

  const login = async (loginId: string, password: string) => {
    const res = await api.post<{ accessToken: string; member: Member }>('/api/auth/login', { loginId, password });
    await AsyncStorage.setItem(TOKEN_KEY, res.accessToken);
    await refresh();
  };

  const signup = async (loginId: string, name: string, password: string) => {
    await api.post('/api/auth/signup', { loginId, name, password });
    await login(loginId, password);
  };

  const logout = async () => {
    await AsyncStorage.removeItem(TOKEN_KEY);
    setMe(null);
  };

  return (
    <AuthContext.Provider value={{ me, isLoading, login, signup, logout, refresh }}>{children}</AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('AuthProvider 밖에서 useAuth를 호출했습니다.');
  return ctx;
}
