import { NoteList } from '@/components/NoteList';
import { colors } from '@/components/ui';

export default function WrongNotesScreen() {
  return (
    <NoteList
      path="/api/me/wrong-notes"
      title="오답노트"
      sub="가장 최근에 틀린 문제예요. 다시 맞히면 자동으로 빠져요"
      emptyText="틀린 문제가 없어요. 잘하고 있어요!"
      icon="close-circle"
      tint={colors.wrong}
      soft={colors.wrongSoft}
    />
  );
}
