import { useCallback, useEffect, useState } from 'react';
import { useWindowDimensions } from 'react-native';

import { ApiError, api } from './api';

/**
 * 웹·모바일 레이아웃 기준. OS가 아니라 화면 너비로 나눈다.
 * 휴대폰 브라우저로 들어와도 모바일 레이아웃이 나와야 하기 때문이다.
 */
export const WIDE_BREAKPOINT = 768;

export function useIsWide() {
  return useWindowDimensions().width >= WIDE_BREAKPOINT;
}

/** GET 요청 하나를 불러온다. path가 null이면 요청하지 않는다. */
export function useApi<T>(path: string | null) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [loading, setLoading] = useState(path !== null);

  const reload = useCallback(async () => {
    if (path === null) return;
    setLoading(true);
    setError(null);
    try {
      setData(await api.get<T>(path));
    } catch (e) {
      setError(e as ApiError);
    } finally {
      setLoading(false);
    }
  }, [path]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, error, loading, reload, setData };
}
