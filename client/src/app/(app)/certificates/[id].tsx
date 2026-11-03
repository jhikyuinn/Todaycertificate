import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { BottomSheet } from '@/components/BottomSheet';
import { IconTile, Ionicons } from '@/components/Icon';
import { Badge, Button, Card, colors, Empty, ErrorBox, H2, Loading, Muted, Screen, Title } from '@/components/ui';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { errorMessage } from '@/lib/errors';
import { formatDate } from '@/lib/format';
import { useApi, useIsWide } from '@/lib/hooks';
import { CertificateDetail, ExamSummary } from '@/lib/types';

/**
 * 자격증 상세: 설명, 시험 기준, 과목, 올해 일정, 회차 목록.
 * 넓은 화면: 2단. 회차마다 [공부] [시험] 버튼을 바로 보여준다 (마우스로 한 번에 선택).
 * 좁은 화면: 회차를 누르면 아래 시트에서 모드를 고른다 (버튼이 작아지지 않게).
 */
export default function CertificateDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const wide = useIsWide();
  const { me, refresh } = useAuth();
  const { data: c, error, loading, reload } = useApi<CertificateDetail>(`/api/certificates/${id}`);
  const [picked, setPicked] = useState<ExamSummary | null>(null);
  const [targetError, setTargetError] = useState<string | null>(null);

  if (loading) return <Loading />;
  if (error || !c) return <Screen><ErrorBox message={errorMessage(error)} onRetry={reload} /></Screen>;

  const setTarget = async (scheduleId: number | null) => {
    setTargetError(null);
    try {
      await api.put('/api/me/target', { scheduleId });
      await refresh(); // 홈의 D-day가 바로 바뀌도록 내 정보를 다시 불러온다
    } catch (e) {
      setTargetError(errorMessage(e));
    }
  };

  const go = (exam: ExamSummary, mode: 'study' | 'exam') => {
    setPicked(null);
    router.push(`/exams/${exam.id}/${mode}`);
  };

  const info = (
    <View style={{ flex: wide ? 1 : undefined, width: wide ? undefined : '100%', gap: 16 }}>
      <Card>
        <H2>시험 기준</H2>
        <View style={s.chips}>
          <Chip icon="time" text={`제한시간 ${c.timeLimitMinutes}분`} />
          <Chip icon="alert-circle" text={`과목 ${c.failScore}점 미만 과락`} />
          <Chip icon="trophy" text={`평균 ${c.passAverage}점 이상 합격`} />
        </View>
        <H2>과목</H2>
        <View style={{ gap: 8 }}>
          {c.subjects.map((sub) => (
            <View key={sub.number} style={s.subjectRow}>
              <View style={s.subjectNo}>
                <Text style={{ fontWeight: '800', color: colors.mint }}>{sub.number}</Text>
              </View>
              <Text style={{ color: colors.text, fontSize: 15 }}>{sub.name}</Text>
            </View>
          ))}
        </View>
      </Card>

      <Card>
        <H2>올해 시험 일정</H2>
        {targetError && <ErrorBox message={targetError} />}
        {c.schedules.length === 0 && <Muted>올해 일정이 아직 없어요</Muted>}
        <View style={{ gap: 10 }}>
          {c.schedules.map((sc) => {
            const isTarget = me?.target?.certificateId === c.id && me.target.round === sc.round && me.target.year === sc.year;
            return (
              <View key={sc.id} style={[s.schedule, !wide && { flexDirection: 'column', alignItems: 'stretch' }, isTarget && { borderColor: colors.primary, backgroundColor: colors.primarySoft }]}>
                <View style={{ flex: wide ? 1 : undefined }}>
                  <Text style={{ fontWeight: '800', color: colors.text }}>
                    {sc.year}년 {sc.round}회 · 시험 {formatDate(sc.examDate)}
                  </Text>
                  <Muted style={{ marginTop: 2 }}>
                    접수 {formatDate(sc.registrationStart)} ~ {formatDate(sc.registrationEnd)}
                  </Muted>
                </View>
                {isTarget ? (
                  <Button title="목표 해제" variant="secondary" onPress={() => setTarget(null)} />
                ) : (
                  <Button title="목표로 설정" variant="ghost" onPress={() => setTarget(sc.id)} />
                )}
              </View>
            );
          })}
        </View>
      </Card>
    </View>
  );

  const exams = (
    <View style={{ flex: wide ? 1.2 : undefined, width: wide ? undefined : '100%' }}>
      <H2>기출 회차</H2>
      {c.exams.length === 0 && <Empty>아직 등록된 기출이 없어요</Empty>}
      <View style={{ gap: 10 }}>
        {c.exams.map((exam) =>
          wide ? (
            <Card key={exam.id} style={s.examRow}>
              <IconTile name="document-text" tint={colors.primary} soft={colors.primarySoft} size={18} />
              <View style={{ flex: 1 }}>
                <Text style={s.examTitle}>
                  {exam.year}년 {exam.round}회
                </Text>
                <Muted>{exam.questionCount}문제</Muted>
              </View>
              <Button title="공부" variant="secondary" onPress={() => go(exam, 'study')} disabled={exam.questionCount === 0} />
              <Button title="시험" onPress={() => go(exam, 'exam')} disabled={exam.questionCount === 0} />
            </Card>
          ) : (
            <Card key={exam.id} style={s.examRow} onPress={exam.questionCount ? () => setPicked(exam) : undefined}>
              <IconTile name="document-text" tint={colors.primary} soft={colors.primarySoft} size={18} />
              <View style={{ flex: 1 }}>
                <Text style={s.examTitle}>
                  {exam.year}년 {exam.round}회
                </Text>
                <Muted>{exam.questionCount}문제</Muted>
              </View>
              <Ionicons name="chevron-forward" size={20} color={colors.sub} />
            </Card>
          ),
        )}
      </View>
    </View>
  );

  return (
    <Screen maxWidth={1080}>
      <Title sub={c.description ?? undefined}>{c.name}</Title>
      {/* 모바일은 회차(바로 풀 것)를 먼저, 웹은 정보와 회차를 나란히 */}
      {wide ? (
        <View style={{ flexDirection: 'row', gap: 24, alignItems: 'flex-start' }}>
          {info}
          {exams}
        </View>
      ) : (
        <View style={{ gap: 24 }}>
          {exams}
          {info}
        </View>
      )}

      <BottomSheet
        visible={picked !== null}
        onClose={() => setPicked(null)}
        title={picked ? `${picked.year}년 ${picked.round}회 · ${picked.questionCount}문제` : ''}>
        {picked && (
          <View style={{ gap: 12 }}>
            <ModeButton
              icon="bulb"
              tint={colors.primary}
              soft={colors.primarySoft}
              title="공부 모드"
              desc="한 문제씩 풀고 바로 정답과 해설 보기"
              onPress={() => go(picked, 'study')}
            />
            <ModeButton
              icon="timer"
              tint={colors.mint}
              soft={colors.mintSoft}
              title="시험 모드"
              desc={`제한시간 ${c.timeLimitMinutes}분, 끝나면 과목별 점수와 합격 여부`}
              onPress={() => go(picked, 'exam')}
            />
          </View>
        )}
      </BottomSheet>
    </Screen>
  );
}

function Chip({ icon, text }: { icon: keyof typeof Ionicons.glyphMap; text: string }) {
  return (
    <View style={s.chip}>
      <Ionicons name={icon} size={14} color={colors.warn} />
      <Text style={{ color: colors.text, fontSize: 13, fontWeight: '600' }}>{text}</Text>
    </View>
  );
}

function ModeButton(props: {
  icon: keyof typeof Ionicons.glyphMap;
  tint: string;
  soft: string;
  title: string;
  desc: string;
  onPress: () => void;
}) {
  return (
    <Pressable onPress={props.onPress} style={[s.mode, { borderColor: props.soft }]}>
      <IconTile name={props.icon} tint={props.tint} soft={props.soft} size={22} />
      <View style={{ flex: 1 }}>
        <Text style={{ fontSize: 17, fontWeight: '800', color: colors.text }}>{props.title}</Text>
        <Muted style={{ marginTop: 2 }}>{props.desc}</Muted>
      </View>
      <Ionicons name="chevron-forward" size={20} color={colors.sub} />
    </Pressable>
  );
}

const s = StyleSheet.create({
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 12 },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: colors.butterSoft,
  },
  subjectRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  subjectNo: {
    width: 28,
    height: 28,
    borderRadius: 9,
    backgroundColor: colors.mintSoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  schedule: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    padding: 12,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: colors.border,
  },
  examRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 14 },
  examTitle: { fontSize: 16, fontWeight: '800', color: colors.text },
  mode: { flexDirection: 'row', alignItems: 'center', gap: 14, padding: 16, borderRadius: 18, borderWidth: 2 },
});
