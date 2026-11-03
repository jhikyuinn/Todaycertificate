import { NoteList } from '@/components/NoteList';
import { colors } from '@/components/ui';

export default function BookmarksScreen() {
  return (
    <NoteList
      path="/api/me/bookmarks"
      title="별표 문제"
      sub="문제 카드의 ☆를 눌러 모아 둔 문제예요"
      emptyText="별표한 문제가 없어요. 풀다가 ☆를 눌러 보세요"
      icon="star"
      tint={colors.warn}
      soft={colors.butterSoft}
    />
  );
}
