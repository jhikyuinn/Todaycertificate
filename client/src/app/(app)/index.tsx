import { Screen, Title } from '@/components/ui';
import { useAuth } from '@/lib/auth';

export default function HomeScreen() {
  const { me } = useAuth();
  return (
    <Screen>
      <Title sub="홈 화면은 다음 단계에서 채운다">{me?.name}님, 안녕하세요</Title>
    </Screen>
  );
}
