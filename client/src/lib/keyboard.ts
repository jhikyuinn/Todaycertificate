import { useEffect, useRef } from 'react';
import { Platform } from 'react-native';

/**
 * 웹에서만 키보드 단축키를 받는다 (넓은 화면 = 키보드가 있는 환경을 가정).
 * 입력칸에 타이핑할 때는 무시한다. handler는 ref로 들고 있어서 매 렌더마다 리스너를 다시 달지 않는다.
 */
export function useKeyboard(handler: (key: string) => void, enabled = true) {
  const ref = useRef(handler);
  ref.current = handler;

  useEffect(() => {
    if (Platform.OS !== 'web' || !enabled) return;
    const onKey = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement | null)?.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA' || e.ctrlKey || e.metaKey || e.altKey) return;
      ref.current(e.key);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [enabled]);
}
