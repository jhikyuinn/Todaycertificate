import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { AdminGate } from '@/components/AdminGate';
import { Ionicons } from '@/components/Icon';
import { Button, Card, colors, ErrorBox, Field, H2, Loading, Muted, Screen, Title } from '@/components/ui';
import { api, ApiError } from '@/lib/api';
import { errorMessage } from '@/lib/errors';
import { formatDate } from '@/lib/format';
import { useApi, useIsWide } from '@/lib/hooks';
import { CertificateDetail } from '@/lib/types';

type SubjectForm = { number: string; name: string };
const EMPTY = { name: '', description: '', timeLimitMinutes: '150', failScore: '40', passAverage: '60' };

/**
 * 자격증 생성(id = new)·수정. 과목은 번호로 맞춰서 서버가 추가·이름 변경·삭제한다.
 * 문제가 등록된 과목을 지우려 하면 서버가 409(SUBJECT_IN_USE)로 막고 아무것도 바꾸지 않는다.
 */
export default function AdminCertificateEdit() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const isNew = id === 'new';
  const wide = useIsWide();
  const detail = useApi<CertificateDetail>(isNew ? null : `/api/certificates/${id}`);
  const [form, setForm] = useState(EMPTY);
  const [subjects, setSubjects] = useState<SubjectForm[]>([{ number: '1', name: '' }]);
  const [errors, setErrors] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  // 수정 화면: 불러온 값으로 입력칸을 채운다
  useEffect(() => {
    const c = detail.data;
    if (!c) return;
    setForm({
      name: c.name,
      description: c.description ?? '',
      timeLimitMinutes: String(c.timeLimitMinutes),
      failScore: String(c.failScore),
      passAverage: String(c.passAverage),
    });
    setSubjects(c.subjects.map((s) => ({ number: String(s.number), name: s.name })));
  }, [detail.data]);

  if (detail.loading) return <Loading />;

  const set = (key: keyof typeof EMPTY) => (v: string) => setForm({ ...form, [key]: v });

  const save = async () => {
    setErrors([]);
    setSaved(false);
    setSaving(true);
    const body = {
      name: form.name.trim(),
      description: form.description.trim() || null,
      timeLimitMinutes: Number(form.timeLimitMinutes),
      failScore: Number(form.failScore),
      passAverage: Number(form.passAverage),
      subjects: subjects.map((s) => ({ number: Number(s.number), name: s.name.trim() })),
    };
    try {
      if (isNew) {
        const res = await api.post<{ id: number }>('/api/admin/certificates', body);
        router.replace(`/admin/certificates/${res.id}`);
      } else {
        await api.put(`/api/admin/certificates/${id}`, body);
        setSaved(true);
        detail.reload();
      }
    } catch (e) {
      // 검증 오류는 어떤 칸이 왜 틀렸는지 목록으로 보여준다
      if (e instanceof ApiError && e.details.length) {
        setErrors(e.details.map((d: any) => `${d.field}: ${d.message}`));
      } else {
        setErrors([errorMessage(e)]);
      }
    } finally {
      setSaving(false);
    }
  };

  const nextNumber = String(Math.max(0, ...subjects.map((s) => Number(s.number) || 0)) + 1);

  const basic = (
    <Card style={{ flex: wide ? 1 : undefined }}>
      <H2>기본 정보</H2>
      <Field label="자격증 이름" value={form.name} onChangeText={set('name')} placeholder="예: 정보처리기사" />
      <Field label="설명" value={form.description} onChangeText={set('description')} multiline style={{ minHeight: 70 }} />
      <View style={{ flexDirection: 'row', gap: 10 }}>
        <View style={{ flex: 1 }}>
          <Field label="제한시간(분)" value={form.timeLimitMinutes} onChangeText={set('timeLimitMinutes')} keyboardType="number-pad" />
        </View>
        <View style={{ flex: 1 }}>
          <Field label="과락 기준" value={form.failScore} onChangeText={set('failScore')} keyboardType="number-pad" />
        </View>
        <View style={{ flex: 1 }}>
          <Field label="합격 평균" value={form.passAverage} onChangeText={set('passAverage')} keyboardType="number-pad" />
        </View>
      </View>
    </Card>
  );

  const subjectCard = (
    <Card style={{ flex: wide ? 1 : undefined }}>
      <H2>과목</H2>
      <Muted style={{ marginBottom: 12 }}>과목번호가 CSV의 '과목번호'와 맞아야 해요</Muted>
      <View style={{ gap: 8 }}>
        {subjects.map((s, i) => (
          <View key={i} style={{ flexDirection: 'row', gap: 8, alignItems: 'center' }}>
            <View style={{ width: 64 }}>
              <Field
                label="번호"
                value={s.number}
                keyboardType="number-pad"
                onChangeText={(v) => setSubjects(subjects.map((x, j) => (j === i ? { ...x, number: v } : x)))}
              />
            </View>
            <View style={{ flex: 1 }}>
              <Field
                label="과목 이름"
                value={s.name}
                onChangeText={(v) => setSubjects(subjects.map((x, j) => (j === i ? { ...x, name: v } : x)))}
              />
            </View>
            <Pressable
              onPress={() => setSubjects(subjects.filter((_, j) => j !== i))}
              disabled={subjects.length === 1}
              accessibilityLabel="과목 삭제"
              style={{ paddingTop: 10, opacity: subjects.length === 1 ? 0.3 : 1 }}>
              <Ionicons name="trash-outline" size={20} color={colors.wrong} />
            </Pressable>
          </View>
        ))}
      </View>
      <Button title="+ 과목 추가" variant="ghost" onPress={() => setSubjects([...subjects, { number: nextNumber, name: '' }])} style={{ alignSelf: 'flex-start' }} />
    </Card>
  );

  return (
    <AdminGate>
      <Screen maxWidth={1080}>
        <Title sub={isNew ? '새 자격증을 만들어요' : '저장하면 바로 사용자 화면에 반영돼요'}>
          {isNew ? '새 자격증' : form.name || '자격증 수정'}
        </Title>
        {errors.length > 0 && <ErrorBox message={errors.join('\n')} />}
        {saved && (
          <View style={s.saved}>
            <Ionicons name="checkmark-circle" size={18} color={colors.correct} />
            <Text style={{ color: colors.correct, fontWeight: '700' }}>저장했어요</Text>
          </View>
        )}
        <View style={{ flexDirection: wide ? 'row' : 'column', gap: 16, alignItems: 'flex-start' }}>
          {basic}
          {subjectCard}
        </View>
        <View style={{ flexDirection: 'row', gap: 10, marginTop: 16 }}>
          <Button title={isNew ? '만들기' : '저장'} onPress={save} loading={saving} />
          {!isNew && <Button title="기출 업로드" variant="secondary" onPress={() => router.push(`/admin/upload?certificateId=${id}`)} />}
        </View>
        {!isNew && detail.data && <Schedules certificateId={Number(id)} detail={detail.data} onChange={detail.reload} />}
      </Screen>
    </AdminGate>
  );
}

/** 시험 일정 추가·삭제. 날짜는 YYYY-MM-DD로 입력한다. 조회 API가 올해 일정만 주므로 목록도 올해만 보인다. */
function Schedules({ certificateId, detail, onChange }: { certificateId: number; detail: CertificateDetail; onChange: () => void }) {
  const wide = useIsWide();
  const thisYear = String(new Date().getFullYear());
  const [f, setF] = useState({ year: thisYear, round: '1', registrationStart: '', registrationEnd: '', examDate: '' });
  const [error, setError] = useState<string | null>(null);

  const add = async () => {
    setError(null);
    try {
      await api.post(`/api/admin/certificates/${certificateId}/schedules`, { ...f, year: Number(f.year), round: Number(f.round) });
      setF({ ...f, registrationStart: '', registrationEnd: '', examDate: '' });
      onChange();
    } catch (e) {
      setError(e instanceof ApiError && e.details.length ? e.details.map((d: any) => d.message).join('\n') : errorMessage(e));
    }
  };

  const remove = async (scheduleId: number) => {
    setError(null);
    try {
      await api.delete(`/api/admin/schedules/${scheduleId}`);
      onChange();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const field = (key: keyof typeof f, label: string, placeholder?: string) => (
    <View style={{ flex: 1, minWidth: wide ? 0 : '45%' }}>
      <Field label={label} value={f[key]} placeholder={placeholder} onChangeText={(v) => setF({ ...f, [key]: v })} />
    </View>
  );

  return (
    <Card style={{ marginTop: 24 }}>
      <H2>올해 시험 일정</H2>
      {error && <ErrorBox message={error} />}
      <View style={{ gap: 8, marginBottom: 16 }}>
        {detail.schedules.length === 0 && <Muted>등록된 일정이 없어요</Muted>}
        {detail.schedules.map((sc) => (
          <View key={sc.id} style={s.scheduleRow}>
            <Text style={{ flex: 1, color: colors.text }}>
              {sc.year}년 {sc.round}회 · 접수 {formatDate(sc.registrationStart)}~{formatDate(sc.registrationEnd)} · 시험 {formatDate(sc.examDate)}
            </Text>
            <Pressable onPress={() => remove(sc.id)} accessibilityLabel="일정 삭제">
              <Ionicons name="trash-outline" size={18} color={colors.wrong} />
            </Pressable>
          </View>
        ))}
      </View>
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8 }}>
        {field('year', '연도')}
        {field('round', '회차')}
        {field('registrationStart', '접수 시작', 'YYYY-MM-DD')}
        {field('registrationEnd', '접수 마감', 'YYYY-MM-DD')}
        {field('examDate', '시험일', 'YYYY-MM-DD')}
      </View>
      <Button title="+ 일정 추가" variant="secondary" onPress={add} style={{ alignSelf: 'flex-start' }} />
    </Card>
  );
}

const s = StyleSheet.create({
  saved: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 12 },
  scheduleRow: { flexDirection: 'row', alignItems: 'center', gap: 10, padding: 10, borderRadius: 12, backgroundColor: colors.bg },
});
