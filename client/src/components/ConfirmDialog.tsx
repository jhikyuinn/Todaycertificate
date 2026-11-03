import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import { Button, colors } from './ui';

/** 가운데 확인 창. window.confirm은 웹에서만 되므로 직접 만든다 (모바일 앱에서도 같은 모양). */
export function ConfirmDialog({
  visible,
  title,
  message,
  confirmText,
  onConfirm,
  onCancel,
  loading,
}: {
  visible: boolean;
  title: string;
  message?: string;
  confirmText: string;
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
}) {
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <View style={s.backdrop}>
        <Pressable style={StyleSheet.absoluteFill} onPress={onCancel} />
        <View style={s.box}>
          <Text style={s.title}>{title}</Text>
          {message ? <Text style={s.message}>{message}</Text> : null}
          <View style={{ flexDirection: 'row', gap: 10, marginTop: 20 }}>
            <Button title="취소" variant="secondary" onPress={onCancel} style={{ flex: 1 }} />
            <Button title={confirmText} onPress={onConfirm} loading={loading} style={{ flex: 1 }} />
          </View>
        </View>
      </View>
    </Modal>
  );
}

const s = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(59, 52, 46, 0.35)', alignItems: 'center', justifyContent: 'center', padding: 24 },
  box: { width: '100%', maxWidth: 380, backgroundColor: colors.card, borderRadius: 22, padding: 24 },
  title: { fontSize: 19, fontWeight: '800', color: colors.text },
  message: { fontSize: 15, color: colors.sub, marginTop: 8, lineHeight: 22 },
});
