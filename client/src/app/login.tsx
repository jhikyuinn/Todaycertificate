import { Link } from 'expo-router';
import { useState } from 'react';
import { Text, View } from 'react-native';

import { AuthForm } from '@/components/AuthForm';
import { Button, colors, ErrorBox, Field, H2 } from '@/components/ui';
import { useAuth } from '@/lib/auth';
import { errorMessage } from '@/lib/errors';

export default function LoginScreen() {
  const { login } = useAuth();
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // 로그인에 성공하면 Stack.Protected가 알아서 (app)으로 보낸다
  const submit = async () => {
    setError(null);
    setLoading(true);
    try {
      await login(loginId.trim(), password);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthForm>
      <H2>로그인</H2>
      {error && <ErrorBox message={error} />}
      <Field label="아이디" value={loginId} onChangeText={setLoginId} autoCapitalize="none" autoComplete="username" />
      <Field
        label="비밀번호"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        autoComplete="current-password"
        onSubmitEditing={submit}
      />
      <Button title="로그인" onPress={submit} loading={loading} disabled={!loginId || !password} big />
      <View style={{ flexDirection: 'row', justifyContent: 'center', marginTop: 16, gap: 6 }}>
        <Text style={{ color: colors.sub }}>처음이신가요?</Text>
        <Link href="/signup" style={{ color: colors.primary, fontWeight: '700' }}>
          회원가입
        </Link>
      </View>
    </AuthForm>
  );
}
