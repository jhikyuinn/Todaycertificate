import { Link } from 'expo-router';
import { useState } from 'react';
import { Text, View } from 'react-native';

import { AuthForm } from '@/components/AuthForm';
import { Button, colors, ErrorBox, Field, H2 } from '@/components/ui';
import { ApiError } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { errorMessage, fieldErrors } from '@/lib/errors';

export default function SignupScreen() {
  const { signup } = useAuth();
  const [form, setForm] = useState({ loginId: '', name: '', password: '' });
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  const set = (key: keyof typeof form) => (value: string) => setForm({ ...form, [key]: value });

  // 서버가 최종 검증한다. 화면에서는 같은 규칙으로 미리 안내만 한다
  const submit = async () => {
    setError(null);
    setFields({});
    setLoading(true);
    try {
      await signup(form.loginId.trim(), form.name.trim(), form.password);
    } catch (e) {
      const f = e instanceof ApiError && e.code === 'DUPLICATE_LOGIN_ID' ? { loginId: errorMessage(e) } : fieldErrors(e);
      if (Object.keys(f).length > 0) setFields(f);
      else setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthForm>
      <H2>회원가입</H2>
      {error && <ErrorBox message={error} />}
      <Field
        label="아이디"
        placeholder="영문·숫자 4~20자"
        value={form.loginId}
        onChangeText={set('loginId')}
        autoCapitalize="none"
        error={fields.loginId}
      />
      <Field label="이름" value={form.name} onChangeText={set('name')} error={fields.name} />
      <Field
        label="비밀번호"
        placeholder="8~64자"
        value={form.password}
        onChangeText={set('password')}
        secureTextEntry
        autoComplete="new-password"
        error={fields.password}
        onSubmitEditing={submit}
      />
      <Button
        title="가입하고 시작하기"
        onPress={submit}
        loading={loading}
        disabled={!form.loginId || !form.name || !form.password}
        big
      />
      <View style={{ flexDirection: 'row', justifyContent: 'center', marginTop: 16, gap: 6 }}>
        <Text style={{ color: colors.sub }}>이미 계정이 있나요?</Text>
        <Link href="/login" style={{ color: colors.primary, fontWeight: '700' }}>
          로그인
        </Link>
      </View>
    </AuthForm>
  );
}
