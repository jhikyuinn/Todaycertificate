import AsyncStorage from '@react-native-async-storage/async-storage';

const BASE_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';
export const TOKEN_KEY = 'accessToken';

/** 서버 공통 오류 형식 {code, message, details}를 그대로 담는다. */
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public details: any[] = [],
  ) {
    super(message);
  }
}

// 401을 받으면 로그인 상태를 지우도록 auth.tsx가 등록한다
let onUnauthorized: () => void = () => {};
export function setOnUnauthorized(handler: () => void) {
  onUnauthorized = handler;
}

/**
 * 토큰을 붙여 요청하고, 2xx가 아니면 ApiError를 던진다.
 * body가 FormData면 Content-Type을 브라우저에 맡긴다 (multipart 경계값을 직접 못 정하므로).
 */
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = await AsyncStorage.getItem(TOKEN_KEY);
  const headers: Record<string, string> = { ...(init.headers as Record<string, string>) };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (init.body && !(init.body instanceof FormData)) headers['Content-Type'] = 'application/json';

  let res: Response;
  try {
    res = await fetch(BASE_URL + path, { ...init, headers });
  } catch {
    throw new ApiError(0, 'NETWORK', '서버에 연결할 수 없습니다.');
  }

  if (res.status === 401 && token) {
    await AsyncStorage.removeItem(TOKEN_KEY);
    onUnauthorized();
  }
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new ApiError(res.status, body?.code ?? 'UNKNOWN', body?.message ?? '요청에 실패했습니다.', body?.details ?? []);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

export const api = {
  get: <T>(path: string) => apiFetch<T>(path),
  post: <T>(path: string, body?: unknown) =>
    apiFetch<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) }),
  put: <T>(path: string, body?: unknown) =>
    apiFetch<T>(path, { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) }),
  delete: <T>(path: string) => apiFetch<T>(path, { method: 'DELETE' }),
};
