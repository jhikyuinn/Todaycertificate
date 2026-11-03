package com.todaycertificate.exam;

import com.todaycertificate.solve.BookmarkRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 문제 목록 → QuestionView 목록. 별표 여부는 회원마다 다르므로 내 별표 ID를 쿼리 한 번으로 가져와 매핑한다.
 * 문제 조회(#7), 시험 시작(#13), 오답·별표 목록(#15)이 함께 쓴다. 문제의 과목은 미리 fetch되어 있어야 한다.
 */
@Component
public class QuestionViewAssembler {

    private final BookmarkRepository bookmarks;

    public QuestionViewAssembler(BookmarkRepository bookmarks) {
        this.bookmarks = bookmarks;
    }

    public List<QuestionView> toViews(Long memberId, List<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        Set<Long> bookmarked = bookmarks.findBookmarkedQuestionIds(memberId,
                questions.stream().map(Question::getId).toList());
        return questions.stream().map(q -> QuestionView.of(q, bookmarked.contains(q.getId()))).toList();
    }
}
