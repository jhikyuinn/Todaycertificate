import { Platform, ScrollView, StyleProp, StyleSheet, Text, TextStyle, View } from 'react-native';

import { colors } from './ui';

export type Segment = { type: 'text' | 'code'; value: string };

/**
 * 문제 본문을 일반 글과 코드 블록으로 나눈다. 코드는 ``` 로 감싼다 (```java 처럼 언어 이름은 무시).
 * 닫는 ``` 가 없으면 끝까지 코드로 본다 (운영자가 실수해도 글이 사라지지 않게).
 */
export function splitCodeBlocks(text: string): Segment[] {
  const segments: Segment[] = [];
  const parts = text.split('```');
  parts.forEach((part, i) => {
    if (i % 2 === 0) {
      const t = part.replace(/^\n+|\n+$/g, '');
      if (t) segments.push({ type: 'text', value: t });
    } else {
      // 여는 ``` 뒤의 언어 이름 줄을 떼고, 앞뒤 빈 줄만 지운다 (들여쓰기는 유지)
      const code = part.replace(/^[a-zA-Z0-9+#-]*\n/, '').replace(/^\n+|\s+$/g, '');
      segments.push({ type: 'code', value: code });
    }
  });
  return segments;
}

const MONO = Platform.select({
  ios: 'Menlo',
  android: 'monospace',
  default: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
});

export function RichText({ text, style }: { text: string; style?: StyleProp<TextStyle> }) {
  return (
    <View style={{ gap: 10 }}>
      {splitCodeBlocks(text).map((seg, i) =>
        seg.type === 'text' ? (
          <Text key={i} style={[s.text, style]}>
            {seg.value}
          </Text>
        ) : (
          // 긴 코드 줄은 줄바꿈하지 않고 가로로 스크롤한다 (줄바꿈하면 들여쓰기 구조가 깨진다)
          <ScrollView key={i} horizontal style={s.codeBox} contentContainerStyle={{ padding: 12 }}>
            <Text style={s.code}>{seg.value}</Text>
          </ScrollView>
        ),
      )}
    </View>
  );
}

const s = StyleSheet.create({
  text: { fontSize: 16, lineHeight: 25, color: colors.text },
  codeBox: { backgroundColor: '#0F172A', borderRadius: 8 },
  code: { fontFamily: MONO, fontSize: 14, lineHeight: 21, color: '#E2E8F0' },
});
