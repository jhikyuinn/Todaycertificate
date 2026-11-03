import { ReactNode } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleProp,
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  TextStyle,
  View,
  ViewStyle,
} from 'react-native';

import { useIsWide } from '@/lib/hooks';

/**
 * 앱 전체 색. 웹과 모바일이 같은 색을 쓰고, 배치만 다르다.
 * 크림색 바탕 + 귤색 포인트 + 민트·버터 보조색: 공부 앱이 딱딱해 보이지 않게 따뜻하고 부드러운 톤.
 */
export const colors = {
  primary: '#F2762E', // 귤색: 주요 버튼, 선택
  primarySoft: '#FFE9D9',
  mint: '#3FA58A', // 보조 강조: 진행, 성공에 가까운 정보
  mintSoft: '#DDF3EC',
  butter: '#FFD66B', // 별표, 하이라이트
  butterSoft: '#FFF4D1',
  text: '#3B342E', // 검정 대신 따뜻한 갈회색
  sub: '#8C8178',
  border: '#EFE4D6',
  bg: '#FFF9F1',
  card: '#FFFFFF',
  correct: '#2E9E6A',
  correctSoft: '#DCF4E8',
  wrong: '#E5534B',
  wrongSoft: '#FDE3E1',
  warn: '#D98A00',
  warnSoft: '#FFF0C7',
};

/**
 * 화면 본문. 넓은 화면에서는 가운데 정렬하고 최대 폭을 둔다 (한 줄이 너무 길면 읽기 어렵다).
 * footer는 모바일 하단 고정 버튼 영역이다.
 */
export function Screen({
  children,
  maxWidth = 960,
  scroll = true,
  footer,
}: {
  children: ReactNode;
  maxWidth?: number;
  scroll?: boolean;
  footer?: ReactNode;
}) {
  const wide = useIsWide();
  const inner = (
    <View style={[styles.inner, { maxWidth, padding: wide ? 32 : 16 }]}>{children}</View>
  );
  return (
    <View style={styles.screen}>
      {scroll ? <ScrollView contentContainerStyle={styles.scrollContent}>{inner}</ScrollView> : inner}
      {footer && <View style={styles.footer}>{footer}</View>}
    </View>
  );
}

export function Title({ children, sub }: { children: ReactNode; sub?: ReactNode }) {
  const wide = useIsWide();
  return (
    <View style={{ marginBottom: wide ? 24 : 16 }}>
      <Text style={[styles.title, { fontSize: wide ? 28 : 22 }]}>{children}</Text>
      {sub ? <Text style={styles.sub}>{sub}</Text> : null}
    </View>
  );
}

export function H2({ children }: { children: ReactNode }) {
  return <Text style={styles.h2}>{children}</Text>;
}

export function Body({ children, style }: { children: ReactNode; style?: StyleProp<TextStyle> }) {
  return <Text style={[styles.body, style]}>{children}</Text>;
}

export function Muted({ children, style }: { children: ReactNode; style?: StyleProp<TextStyle> }) {
  return <Text style={[styles.muted, style]}>{children}</Text>;
}

export function Card({
  children,
  style,
  onPress,
}: {
  children: ReactNode;
  style?: StyleProp<ViewStyle>;
  onPress?: () => void;
}) {
  if (onPress) {
    return (
      <Pressable
        onPress={onPress}
        style={({ pressed, hovered }: any) => [styles.card, (pressed || hovered) && styles.cardHover, style]}>
        {children}
      </Pressable>
    );
  }
  return <View style={[styles.card, style]}>{children}</View>;
}

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost';

export function Button({
  title,
  onPress,
  variant = 'primary',
  disabled,
  loading,
  style,
  big,
}: {
  title: string;
  onPress: () => void;
  variant?: ButtonVariant;
  disabled?: boolean;
  loading?: boolean;
  style?: StyleProp<ViewStyle>;
  big?: boolean; // 모바일 하단 버튼처럼 엄지로 누르기 쉬운 크기
}) {
  const v = buttonVariants[variant];
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      disabled={disabled || loading}
      style={({ pressed, hovered }: any) => [
        styles.button,
        big && styles.buttonBig,
        { backgroundColor: v.bg, borderColor: v.border },
        (pressed || hovered) && { opacity: 0.85 },
        (disabled || loading) && { opacity: 0.5 },
        style,
      ]}>
      {loading ? (
        <ActivityIndicator color={v.text} />
      ) : (
        <Text style={[styles.buttonText, big && { fontSize: 17 }, { color: v.text }]}>{title}</Text>
      )}
    </Pressable>
  );
}

const buttonVariants: Record<ButtonVariant, { bg: string; text: string; border: string }> = {
  primary: { bg: colors.primary, text: '#fff', border: colors.primary },
  secondary: { bg: colors.card, text: colors.text, border: colors.border },
  danger: { bg: colors.wrong, text: '#fff', border: colors.wrong },
  ghost: { bg: 'transparent', text: colors.primary, border: 'transparent' },
};

export function Field({ label, error, ...props }: TextInputProps & { label: string; error?: string }) {
  return (
    <View style={{ marginBottom: 14 }}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        placeholderTextColor={colors.sub}
        {...props}
        style={[styles.input, error && { borderColor: colors.wrong }, props.style]}
      />
      {error ? <Text style={styles.fieldError}>{error}</Text> : null}
    </View>
  );
}

export function Badge({ text, tone = 'primary' }: { text: string; tone?: 'primary' | 'correct' | 'wrong' | 'warn' }) {
  const map = {
    primary: [colors.primarySoft, colors.primary],
    correct: [colors.correctSoft, colors.correct],
    wrong: [colors.wrongSoft, colors.wrong],
    warn: [colors.warnSoft, colors.warn],
  } as const;
  const [bg, fg] = map[tone];
  return (
    <View style={[styles.badge, { backgroundColor: bg }]}>
      <Text style={{ color: fg, fontSize: 12, fontWeight: '700' }}>{text}</Text>
    </View>
  );
}

export function Loading() {
  return (
    <View style={styles.center}>
      <ActivityIndicator color={colors.primary} size="large" />
    </View>
  );
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <View style={styles.errorBox}>
      <Text style={{ color: colors.wrong, flex: 1 }}>{message}</Text>
      {onRetry && <Button title="다시 시도" variant="secondary" onPress={onRetry} />}
    </View>
  );
}

export function Empty({ children }: { children: ReactNode }) {
  return (
    <View style={styles.empty}>
      <Text style={styles.muted}>{children}</Text>
    </View>
  );
}

/** 넓은 화면에서는 가로로, 좁은 화면에서는 세로로 쌓는다. */
export function Columns({ children, gap = 24 }: { children: ReactNode; gap?: number }) {
  const wide = useIsWide();
  return <View style={{ flexDirection: wide ? 'row' : 'column', gap, alignItems: 'flex-start' }}>{children}</View>;
}

export const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  scrollContent: { flexGrow: 1 },
  inner: { width: '100%', alignSelf: 'center' },
  footer: {
    padding: 12,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.card,
  },
  title: { fontWeight: '800', color: colors.text, letterSpacing: -0.5 },
  sub: { color: colors.sub, marginTop: 6, fontSize: 15 },
  h2: { fontSize: 18, fontWeight: '700', color: colors.text, marginBottom: 12, marginTop: 8 },
  body: { fontSize: 16, color: colors.text, lineHeight: 24 },
  muted: { fontSize: 14, color: colors.sub },
  card: {
    backgroundColor: colors.card,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: colors.border,
    padding: 18,
  },
  cardHover: { borderColor: colors.primary },
  button: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 14,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonBig: { paddingVertical: 15, borderRadius: 16 },
  buttonText: { fontSize: 15, fontWeight: '700' },
  label: { fontSize: 14, fontWeight: '600', color: colors.text, marginBottom: 6 },
  input: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 16,
    backgroundColor: colors.card,
    color: colors.text,
  },
  fieldError: { color: colors.wrong, fontSize: 13, marginTop: 4 },
  badge: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 999, alignSelf: 'flex-start' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 40 },
  errorBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 14,
    borderRadius: 10,
    backgroundColor: colors.wrongSoft,
    marginBottom: 16,
  },
  empty: { padding: 32, alignItems: 'center' },
});
