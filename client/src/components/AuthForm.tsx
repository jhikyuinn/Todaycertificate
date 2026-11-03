import Ionicons from '@expo/vector-icons/Ionicons';
import { ReactNode } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';

import { useIsWide } from '@/lib/hooks';

import { colors } from './ui';

/**
 * 로그인·회원가입 공통 틀.
 * 넓은 화면: 왼쪽에 앱 소개, 오른쪽에 입력 카드 (처음 온 사람에게 무슨 앱인지 보여준다).
 * 좁은 화면: 소개를 짧게 줄이고 입력칸을 바로 보여준다.
 */
export function AuthForm({ children }: { children: ReactNode }) {
  const wide = useIsWide();
  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={[s.page, wide && s.pageWide]}>
        <View style={[s.intro, wide && s.introWide]}>
          <View style={[s.mark, wide && { width: 56, height: 56, borderRadius: 18 }]}>
            <Ionicons name="sunny" size={wide ? 32 : 26} color="#fff" />
          </View>
          <Text style={[s.brand, wide && { fontSize: 40 }]}>오늘의 자격증</Text>
          <Text style={s.slogan}>시험일까지, 오늘 풀 문제를 골라주는{'\n'}국가자격 필기 기출 학습</Text>
          {wide && (
            <View style={{ marginTop: 28, gap: 10 }}>
              <Point icon="bulb" tint={colors.primary} soft={colors.primarySoft} text="공부 모드: 한 문제씩 풀고 바로 해설" />
              <Point icon="timer" tint={colors.mint} soft={colors.mintSoft} text="시험 모드: 실제 제한시간, 과목별 과락·합격" />
              <Point icon="sparkles" tint={colors.warn} soft={colors.butterSoft} text="오늘의 문제: 오답과 약한 과목부터" />
            </View>
          )}
        </View>
        <View style={[s.card, wide && s.cardWide]}>{children}</View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

function Point({ icon, tint, soft, text }: { icon: keyof typeof Ionicons.glyphMap; tint: string; soft: string; text: string }) {
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
      <View style={{ width: 32, height: 32, borderRadius: 10, backgroundColor: soft, alignItems: 'center', justifyContent: 'center' }}>
        <Ionicons name={icon} size={17} color={tint} />
      </View>
      <Text style={s.point}>{text}</Text>
    </View>
  );
}

const s = StyleSheet.create({
  mark: {
    width: 46,
    height: 46,
    borderRadius: 15,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 14,
  },
  page: { flexGrow: 1, padding: 20, justifyContent: 'center', backgroundColor: colors.bg },
  pageWide: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 64, padding: 48 },
  intro: { marginBottom: 24 },
  introWide: { maxWidth: 420, marginBottom: 0 },
  brand: { fontSize: 30, fontWeight: '900', color: colors.text, letterSpacing: -1 },
  slogan: { fontSize: 16, color: colors.sub, marginTop: 8, lineHeight: 24 },
  point: { fontSize: 15, color: colors.text },
  card: {
    backgroundColor: colors.card,
    borderRadius: 22,
    borderWidth: 1,
    borderColor: colors.border,
    padding: 20,
  },
  cardWide: { width: 400, padding: 32 },
});
