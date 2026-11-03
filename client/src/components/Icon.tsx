import Ionicons from '@expo/vector-icons/Ionicons';
import { View } from 'react-native';

export type IconName = keyof typeof Ionicons.glyphMap;

/** 둥근 파스텔 사각형 안의 아이콘. 메뉴, 카드 머리, 통계 등에 같은 모양으로 쓴다. */
export function IconTile({ name, tint, soft, size = 20 }: { name: IconName; tint: string; soft: string; size?: number }) {
  return (
    <View
      style={{
        width: size + 20,
        height: size + 20,
        borderRadius: (size + 20) / 3,
        backgroundColor: soft,
        alignItems: 'center',
        justifyContent: 'center',
      }}>
      <Ionicons name={name} size={size} color={tint} />
    </View>
  );
}

export { Ionicons };
