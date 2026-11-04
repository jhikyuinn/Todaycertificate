import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Ionicons } from '@/components/Icon';
import { Badge, Card, colors, Field, Muted } from '@/components/ui';
import { PreviewRow } from '@/lib/types';

const toNumber = (t: string) => Number(t.replace(/\D/g, '')) || 0;

/**
 * PDF 미리보기 행을 저장 전에 고치는 표. 경고가 있는 행을 위로 모은다.
 * 정렬은 화면에서만 하고, onChange에는 원래 순서의 배열을 돌려준다.
 */
export function PreviewTable({ rows, onChange }: { rows: PreviewRow[]; onChange: (rows: PreviewRow[]) => void }) {
  const ordered = rows
    .map((row, index) => ({ row, index }))
    .sort((a, b) => Number(b.row.warnings.length > 0) - Number(a.row.warnings.length > 0));

  const update = (index: number, patch: Partial<PreviewRow>) =>
    onChange(rows.map((r, i) => (i === index ? { ...r, ...patch } : r)));
  const setChoice = (index: number, choice: number, value: string) => {
    const choices = [0, 1, 2, 3].map((i) => rows[index].choices[i] ?? '');
    choices[choice] = value;
    update(index, { choices });
  };

  return (
    <View style={{ gap: 12 }}>
      {ordered.map(({ row, index }) => (
        <Card key={index} style={{ gap: 4 }}>
          <View style={s.head}>
            <Text style={s.title}>{row.number}번</Text>
            {row.type === 'SHORT_ANSWER' && <Badge text="주관식" />}
            {row.warnings.length > 0 && <Badge text="확인 필요" tone="warn" />}
            <View style={{ flex: 1 }} />
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`${row.number}번 문제 삭제`}
              onPress={() => onChange(rows.filter((_, i) => i !== index))}
              style={s.delete}>
              <Ionicons name="trash-outline" size={18} color={colors.wrong} />
              <Text style={{ color: colors.wrong, fontWeight: '700' }}>삭제</Text>
            </Pressable>
          </View>
          {row.warnings.map((w, i) => (
            <Muted key={i}>• {w}</Muted>
          ))}
          <View style={s.row}>
            <View style={{ flex: 1 }}>
              <Field label="문제번호" value={row.number ? String(row.number) : ''} onChangeText={(t) => update(index, { number: toNumber(t) })} keyboardType="number-pad" />
            </View>
            <View style={{ flex: 1 }}>
              <Field label="과목번호" value={row.subjectNumber ? String(row.subjectNumber) : ''} onChangeText={(t) => update(index, { subjectNumber: toNumber(t) })} keyboardType="number-pad" />
            </View>
            {row.type !== 'SHORT_ANSWER' && (
              <View style={{ flex: 1 }}>
                <Field label="정답번호" value={row.answerNo ? String(row.answerNo) : ''} onChangeText={(t) => update(index, { answerNo: toNumber(t) || null })} keyboardType="number-pad" />
              </View>
            )}
          </View>
          <Field label="문제" value={row.content} onChangeText={(t) => update(index, { content: t })} multiline style={s.content} />
          {row.type === 'SHORT_ANSWER' ? (
            <Field
              label="정답 (여러 개 인정하면 | 로 구분)"
              value={row.answerText ?? ''}
              onChangeText={(t) => update(index, { answerText: t || null })}
            />
          ) : (
            <View style={s.choices}>
              {[0, 1, 2, 3].map((c) => (
                <View key={c} style={s.choice}>
                  <Field label={`보기${c + 1}`} value={row.choices[c] ?? ''} onChangeText={(t) => setChoice(index, c, t)} />
                </View>
              ))}
            </View>
          )}
          <Field label="해설" value={row.explanation ?? ''} onChangeText={(t) => update(index, { explanation: t || null })} multiline style={{ minHeight: 50 }} />
        </Card>
      ))}
    </View>
  );
}

const s = StyleSheet.create({
  head: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 4 },
  title: { fontSize: 17, fontWeight: '800', color: colors.text },
  delete: { flexDirection: 'row', alignItems: 'center', gap: 4, padding: 6 },
  row: { flexDirection: 'row', gap: 10, marginTop: 8 },
  choices: { flexDirection: 'row', flexWrap: 'wrap', columnGap: 10 },
  choice: { flexGrow: 1, flexBasis: 220 },
  // 코드(``` 블록)가 들어갈 수 있어 고정폭으로, 들여쓰기가 보이게
  content: { minHeight: 70, fontFamily: 'ui-monospace, Menlo, Consolas, monospace', fontSize: 13 },
});
