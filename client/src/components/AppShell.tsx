import Ionicons from '@expo/vector-icons/Ionicons';
import { Href, router, usePathname } from 'expo-router';
import { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/lib/auth';
import { useIsWide } from '@/lib/hooks';

import { colors } from './ui';

type IconName = keyof typeof Ionicons.glyphMap;
type NavItem = {
  href: Href;
  label: string;
  icon: IconName;
  tint: string; // 메뉴마다 고유 색: 아이콘만 봐도 어느 메뉴인지 구분된다
  soft: string;
  match: (path: string) => boolean;
};

const NAV: NavItem[] = [
  { href: '/', label: '홈', icon: 'home', tint: colors.primary, soft: colors.primarySoft, match: (p) => p === '/' },
  {
    href: '/certificates',
    label: '자격증',
    icon: 'library',
    tint: colors.mint,
    soft: colors.mintSoft,
    match: (p) => p.startsWith('/certificates') || p.startsWith('/exams') || p.startsWith('/attempts'),
  },
  {
    href: '/my',
    label: '내 학습',
    icon: 'bookmarks',
    tint: colors.warn,
    soft: colors.butterSoft,
    match: (p) => p === '/my' || p.startsWith('/notes'),
  },
];
const ADMIN_NAV: NavItem = {
  href: '/admin',
  label: '운영자',
  icon: 'construct',
  tint: colors.sub,
  soft: colors.border,
  match: (p) => p.startsWith('/admin'),
};

function NavIcon({ item, active, size }: { item: NavItem; active: boolean; size: number }) {
  return (
    <View style={[s.iconWrap, { backgroundColor: active ? item.tint : item.soft, width: size + 18, height: size + 18 }]}>
      <Ionicons name={item.icon} size={size} color={active ? '#fff' : item.tint} />
    </View>
  );
}

// 모바일에서 하단 탭을 보여줄 최상위 화면. 나머지(문제 풀이 등)는 탭을 숨기고 화면을 넓게 쓴다
const TOP_LEVEL = ['/', '/certificates', '/my'];

/**
 * 로그인 후 모든 화면의 틀. 레이아웃을 화면 너비로 나눈다.
 * - 넓은 화면(웹): 왼쪽 사이드바. 메뉴가 항상 보여서 마우스로 바로 이동한다.
 * - 좁은 화면(모바일): 하단 탭. 엄지가 닿는 아래쪽에 메뉴를 둔다.
 */
export function AppShell({ children }: { children: ReactNode }) {
  const wide = useIsWide();
  const path = usePathname();
  const { me, logout } = useAuth();
  const insets = useSafeAreaInsets();
  const items = me?.role === 'ADMIN' ? [...NAV, ADMIN_NAV] : NAV;

  if (wide) {
    return (
      <View style={s.row}>
        <View style={s.sidebar}>
          <Pressable onPress={() => router.navigate('/')} style={s.brandRow}>
            <View style={s.brandMark}>
              <Ionicons name="sunny" size={18} color="#fff" />
            </View>
            <Text style={s.brand}>오늘의 자격증</Text>
          </Pressable>
          <View style={{ gap: 4, marginTop: 24 }}>
            {items.map((item) => {
              const active = item.match(path);
              return (
                <Pressable
                  key={item.label}
                  onPress={() => router.navigate(item.href)}
                  style={({ hovered }: any) => [
                    s.sideItem,
                    hovered && { backgroundColor: colors.bg },
                    active && { backgroundColor: item.soft },
                  ]}>
                  <NavIcon item={item} active={active} size={18} />
                  <Text style={[s.sideLabel, active && { fontWeight: '800' }]}>{item.label}</Text>
                </Pressable>
              );
            })}
          </View>
          <View style={s.sideFooter}>
            <Text style={s.userName}>{me?.name}님, 오늘도 화이팅</Text>
            <Pressable onPress={logout} style={s.logoutRow}>
              <Ionicons name="log-out-outline" size={16} color={colors.sub} />
              <Text style={s.logout}>로그아웃</Text>
            </Pressable>
          </View>
        </View>
        <View style={{ flex: 1 }}>{children}</View>
      </View>
    );
  }

  const topLevel = TOP_LEVEL.includes(path);
  return (
    <View style={{ flex: 1, paddingTop: insets.top, backgroundColor: colors.card }}>
      {!topLevel && (
        <View style={s.topBar}>
          <Pressable
            hitSlop={12}
            onPress={() => (router.canGoBack() ? router.back() : router.replace('/'))}
            style={s.back}>
            <Ionicons name="chevron-back" size={22} color={colors.text} />
            <Text style={s.backText}>뒤로</Text>
          </Pressable>
        </View>
      )}
      <View style={{ flex: 1 }}>{children}</View>
      {topLevel && (
        <View style={[s.tabBar, { paddingBottom: Math.max(insets.bottom, 8) }]}>
          {items.map((item) => {
            const active = item.match(path);
            return (
              <Pressable key={item.label} onPress={() => router.navigate(item.href)} style={s.tab}>
                <NavIcon item={item} active={active} size={22} />
                <Text style={[s.tabLabel, active && { color: colors.text, fontWeight: '800' }]}>{item.label}</Text>
              </Pressable>
            );
          })}
        </View>
      )}
    </View>
  );
}

const s = StyleSheet.create({
  row: { flex: 1, flexDirection: 'row', backgroundColor: colors.bg },
  sidebar: {
    width: 232,
    padding: 20,
    backgroundColor: colors.card,
    borderRightWidth: 1,
    borderRightColor: colors.border,
  },
  brandRow: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  brandMark: {
    width: 30,
    height: 30,
    borderRadius: 10,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  brand: { fontSize: 19, fontWeight: '900', color: colors.text, letterSpacing: -0.5 },
  iconWrap: { borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  sideItem: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 8, paddingHorizontal: 10, borderRadius: 14 },
  sideLabel: { fontSize: 16, fontWeight: '600', color: colors.text },
  sideFooter: { marginTop: 'auto', gap: 8, padding: 12, borderRadius: 14, backgroundColor: colors.bg },
  userName: { color: colors.text, fontWeight: '700' },
  logoutRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  logout: { color: colors.sub },
  topBar: { height: 44, justifyContent: 'center', paddingHorizontal: 8, borderBottomWidth: 1, borderBottomColor: colors.border },
  back: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 4, paddingVertical: 6, alignSelf: 'flex-start' },
  backText: { fontSize: 16, color: colors.text, fontWeight: '600' },
  tabBar: {
    flexDirection: 'row',
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.card,
    paddingTop: 6,
  },
  tab: { flex: 1, alignItems: 'center', gap: 4, paddingVertical: 4 },
  tabLabel: { fontSize: 13, color: colors.sub, fontWeight: '600' },
});
