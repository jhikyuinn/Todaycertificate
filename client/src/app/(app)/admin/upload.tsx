import { useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Platform, Pressable, StyleSheet, Text, View } from 'react-native';

import { AdminGate } from '@/components/AdminGate';
import { IconTile, Ionicons } from '@/components/Icon';
import { PreviewTable } from '@/components/PreviewTable';
import { Badge, Button, Card, colors, ErrorBox, Field, H2, Muted, Screen, Title } from '@/components/ui';
import { apiFetch, ApiError } from '@/lib/api';
import { toCsv } from '@/lib/csv';
import { errorMessage } from '@/lib/errors';
import { useApi, useIsWide } from '@/lib/hooks';
import { CertificateItem, PreviewRow, UploadResult } from '@/lib/types';

/** 웹 전용: 브라우저 파일 선택 창을 띄워 파일 하나를 고른다. */
function pickFile(accept: string): Promise<File | null> {
  return new Promise((resolve) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.onchange = () => resolve(input.files?.[0] ?? null);
    input.click();
  });
}

/**
 * 기출 업로드: 자격증 선택 → 연도·회차 입력 → 파일 선택 → 업로드.
 * 한 줄이라도 오류가 있으면 서버가 아무것도 저장하지 않고 줄 번호별 오류를 돌려준다.
 * PDF는 미리보기 표에서 고친 뒤 CSV로 바꿔 같은 업로드 API로 보낸다 (저장 경로를 하나로 유지).
 */
export default function AdminUpload() {
  const params = useLocalSearchParams<{ certificateId?: string }>();
  const wide = useIsWide();
  const certs = useApi<CertificateItem[]>('/api/certificates');
  const [certificateId, setCertificateId] = useState<number | null>(params.certificateId ? Number(params.certificateId) : null);
  const [year, setYear] = useState(String(new Date().getFullYear()));
  const [round, setRound] = useState('1');
  const [mode, setMode] = useState<'csv' | 'pdf'>('csv');
  const [file, setFile] = useState<File | null>(null);
  const [pdf, setPdf] = useState<File | null>(null);
  const [explanationPdf, setExplanationPdf] = useState<File | null>(null);
  const [previewing, setPreviewing] = useState(false);
  const [rows, setRows] = useState<PreviewRow[] | null>(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState<UploadResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [rowErrors, setRowErrors] = useState<{ row: number; message: string }[]>([]);

  const clearMessages = () => {
    setResult(null);
    setError(null);
    setRowErrors([]);
  };

  // PDF_INVALID의 details는 이유 문자열, 그 밖에는 {message} 객체다
  const showError = (e: unknown) =>
    setError(
      e instanceof ApiError && e.details.length
        ? e.details.map((d: any) => (typeof d === 'string' ? d : d.message)).join('\n')
        : errorMessage(e),
    );

  const preview = async () => {
    if (!certificateId || !pdf) return;
    clearMessages();
    setRows(null);
    setPreviewing(true);
    const body = new FormData();
    body.append('file', pdf);
    if (explanationPdf) body.append('explanationFile', explanationPdf);
    try {
      const res = await apiFetch<{ rows: PreviewRow[] }>(`/api/admin/certificates/${certificateId}/exams/pdf-preview`, {
        method: 'POST',
        body,
      });
      setRows(res.rows);
    } catch (e) {
      showError(e);
    } finally {
      setPreviewing(false);
    }
  };

  const upload = async (file: File | null) => {
    if (!certificateId || !file) return;
    clearMessages();
    setUploading(true);
    const body = new FormData();
    body.append('year', year);
    body.append('round', round);
    body.append('file', file);
    try {
      setResult(await apiFetch<UploadResult>(`/api/admin/certificates/${certificateId}/exams`, { method: 'POST', body }));
    } catch (e) {
      if (e instanceof ApiError && e.code === 'CSV_INVALID') setRowErrors(e.details);
      else showError(e);
    } finally {
      setUploading(false);
    }
  };

  const form = (
    <Card style={{ flex: wide ? 1 : undefined, gap: 4 }}>
      <H2>1. 자격증</H2>
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 12 }}>
        {certs.data?.map((c) => (
          <Pressable
            key={c.id}
            onPress={() => setCertificateId(c.id)}
            style={[s.pill, certificateId === c.id && { backgroundColor: colors.primary, borderColor: colors.primary }]}>
            <Text style={{ fontWeight: '700', color: certificateId === c.id ? '#fff' : colors.text }}>{c.name}</Text>
          </Pressable>
        ))}
      </View>
      <H2>2. 연도 · 회차</H2>
      <View style={{ flexDirection: 'row', gap: 10 }}>
        <View style={{ flex: 1 }}>
          <Field label="연도" value={year} onChangeText={setYear} keyboardType="number-pad" />
        </View>
        <View style={{ flex: 1 }}>
          <Field label="회차" value={round} onChangeText={setRound} keyboardType="number-pad" />
        </View>
      </View>
      <H2>3. 파일</H2>
      <View style={{ flexDirection: 'row', gap: 8, marginBottom: 12 }}>
        {(['csv', 'pdf'] as const).map((m) => (
          <Pressable
            key={m}
            accessibilityRole="button"
            onPress={() => setMode(m)}
            style={[s.pill, mode === m && { backgroundColor: colors.primary, borderColor: colors.primary }]}>
            <Text style={{ fontWeight: '700', color: mode === m ? '#fff' : colors.text }}>{m.toUpperCase()}</Text>
          </Pressable>
        ))}
      </View>
      {Platform.OS !== 'web' ? (
        <Muted>파일 업로드는 웹에서만 돼요</Muted>
      ) : mode === 'csv' ? (
        <FilePicker file={file} label="눌러서 CSV 파일 고르기" onPress={async () => setFile(await pickFile('.csv,text/csv'))} />
      ) : (
        <View style={{ gap: 10 }}>
          <FilePicker file={pdf} label="문제 PDF 고르기" onPress={async () => setPdf(await pickFile('.pdf,application/pdf'))} />
          <FilePicker
            file={explanationPdf}
            label="해설 PDF 고르기 (선택)"
            onPress={async () => setExplanationPdf(await pickFile('.pdf,application/pdf'))}
          />
        </View>
      )}
      {mode === 'csv' ? (
        <Button
          title="업로드"
          onPress={() => upload(file)}
          loading={uploading}
          disabled={!certificateId || !file}
          big
          style={{ marginTop: 16 }}
        />
      ) : (
        <Button
          title="미리보기"
          onPress={preview}
          loading={previewing}
          disabled={!certificateId || !pdf}
          big
          style={{ marginTop: 16 }}
        />
      )}
    </Card>
  );

  const guide = (
    <Card style={{ width: wide ? 340 : '100%', backgroundColor: colors.butterSoft, borderColor: 'transparent' }}>
      <H2>CSV 형식</H2>
      <Text style={s.mono}>과목번호,문제번호,문제,보기1,보기2,보기3,보기4,정답번호,해설</Text>
      <View style={{ gap: 6, marginTop: 12 }}>
        <Muted>• UTF-8로 저장 (엑셀은 'CSV UTF-8')</Muted>
        <Muted>• 쉼표·줄바꿈이 있는 칸은 큰따옴표로 감싸기</Muted>
        <Muted>• 코드는 ``` 로 감싸면 고정폭으로 보여요</Muted>
        <Muted>• 주관식은 끝에 '유형' 칸 추가(객관식/주관식/주관식-정확히), 보기는 비우고 정답번호 칸에 답을 '|'로 구분</Muted>
        <Muted>• 같은 회차를 다시 올리면 문제번호 기준으로 덮어써요 (풀이 기록 유지)</Muted>
      </View>
    </Card>
  );

  return (
    <AdminGate>
      <Screen maxWidth={1080}>
        <Title sub="오류가 한 줄이라도 있으면 아무것도 저장하지 않아요">기출 업로드</Title>
        {error && <ErrorBox message={error} />}
        {result && (
          <Card style={[s.result, { backgroundColor: colors.correctSoft }]}>
            <IconTile name="checkmark-circle" tint={colors.correct} soft={colors.card} />
            <View style={{ flex: 1 }}>
              <Text style={{ fontSize: 17, fontWeight: '800', color: colors.text }}>업로드 완료</Text>
              <View style={{ flexDirection: 'row', gap: 6, marginTop: 6 }}>
                <Badge text={`${result.created}개 추가`} tone="correct" />
                <Badge text={`${result.updated}개 수정`} tone="warn" />
                <Badge text={`${result.deleted}개 삭제`} tone="wrong" />
              </View>
            </View>
          </Card>
        )}
        {rowErrors.length > 0 && (
          <Card style={[s.result, { backgroundColor: colors.wrongSoft, alignItems: 'flex-start' }]}>
            <Ionicons name="alert-circle" size={24} color={colors.wrong} />
            <View style={{ flex: 1, gap: 4 }}>
              <Text style={{ fontSize: 17, fontWeight: '800', color: colors.wrong }}>
                오류 {rowErrors.length}개 · 아무것도 저장하지 않았어요
              </Text>
              {rowErrors.map((e, i) => (
                <Text key={i} style={{ color: colors.text }}>
                  {e.row > 0 ? `${e.row}번째 문제: ` : ''}
                  {e.message}
                </Text>
              ))}
            </View>
          </Card>
        )}
        <View style={{ flexDirection: wide ? 'row' : 'column', gap: 16, alignItems: 'flex-start' }}>
          {form}
          {guide}
        </View>
        {mode === 'pdf' && rows && (
          <View style={{ marginTop: 24, gap: 12 }}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12 }}>
              <View style={{ flex: 1 }}>
                <H2>미리보기 {rows.length}문제</H2>
                <Muted>
                  저장하면 이 내용으로 {year}년 {round}회를 업로드해요
                </Muted>
              </View>
              <Button
                title="저장"
                onPress={() => upload(new File([toCsv(rows)], 'pdf-import.csv', { type: 'text/csv' }))}
                loading={uploading}
                disabled={!certificateId || rows.length === 0}
              />
            </View>
            <PreviewTable rows={rows} onChange={setRows} />
          </View>
        )}
      </Screen>
    </AdminGate>
  );
}

function FilePicker({ file, label, onPress }: { file: File | null; label: string; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} style={s.drop}>
      <Ionicons name={file ? 'document-attach' : 'cloud-upload-outline'} size={28} color={colors.primary} />
      <Text style={{ fontWeight: '700', color: colors.text }}>{file ? file.name : label}</Text>
      {file && <Muted>{(file.size / 1024).toFixed(1)}KB · 다시 누르면 바꿀 수 있어요</Muted>}
    </Pressable>
  );
}

const s = StyleSheet.create({
  pill: { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 999, borderWidth: 1.5, borderColor: colors.border },
  drop: {
    alignItems: 'center',
    gap: 6,
    padding: 24,
    borderRadius: 18,
    borderWidth: 2,
    borderStyle: 'dashed',
    borderColor: colors.primarySoft,
    backgroundColor: colors.bg,
  },
  mono: { fontFamily: 'ui-monospace, Menlo, Consolas, monospace', fontSize: 12, color: colors.text },
  result: { flexDirection: 'row', alignItems: 'center', gap: 14, marginBottom: 16, borderColor: 'transparent' },
});
