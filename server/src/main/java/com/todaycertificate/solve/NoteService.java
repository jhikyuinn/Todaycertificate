package com.todaycertificate.solve;

import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.ErrorCode;
import com.todaycertificate.exam.Question;
import com.todaycertificate.exam.QuestionRepository;
import com.todaycertificate.exam.QuestionView;
import com.todaycertificate.exam.QuestionViewAssembler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 별표와 오답노트. 둘 다 "문제 ID 목록 → 자격증·회차 정보가 붙은 문제 목록"으로 끝난다. */
@Service
public class NoteService {

    private final BookmarkRepository bookmarks;
    private final SolveRecordRepository records;
    private final QuestionRepository questions;
    private final QuestionViewAssembler assembler;

    public NoteService(BookmarkRepository bookmarks, SolveRecordRepository records, QuestionRepository questions,
                       QuestionViewAssembler assembler) {
        this.bookmarks = bookmarks;
        this.records = records;
        this.questions = questions;
        this.assembler = assembler;
    }

    public record NoteItem(String certificateName, int examYear, int examRound, QuestionView question) {
    }

    @Transactional
    public void bookmark(Long memberId, Long questionId) {
        if (!questions.existsById(questionId)) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        bookmarks.insertIfAbsent(memberId, questionId);
    }

    @Transactional
    public void unbookmark(Long memberId, Long questionId) {
        bookmarks.deleteByMemberAndQuestion(memberId, questionId);
    }

    /** 최근에 별표한 것부터. */
    @Transactional(readOnly = true)
    public List<NoteItem> bookmarks(Long memberId) {
        return toItems(memberId, bookmarks.findQuestionIdsByMemberId(memberId));
    }

    /** 문제마다 가장 최근 풀이가 오답인 것. 공부·시험 모드 모두 포함하고, 다시 맞히면 자연히 빠진다. */
    @Transactional(readOnly = true)
    public List<NoteItem> wrongNotes(Long memberId) {
        return toItems(memberId, records.findLatestPerQuestion(memberId).stream()
                .filter(latest -> !latest.getCorrect())
                .map(SolveRecordRepository.LatestSolve::getQuestionId)
                .toList());
    }

    private List<NoteItem> toItems(Long memberId, List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return List.of();
        }
        // IN 쿼리는 순서를 보장하지 않으므로 받은 ID 순서대로 다시 줄 세운다
        Map<Long, Question> byId = questions.findWithDetailsByIdIn(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        List<Question> ordered = questionIds.stream().map(byId::get).filter(Objects::nonNull).toList();
        List<QuestionView> views = assembler.toViews(memberId, ordered);

        List<NoteItem> items = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            Question q = ordered.get(i);
            items.add(new NoteItem(q.getExam().getCertificate().getName(), q.getExam().getYear(),
                    q.getExam().getRound(), views.get(i)));
        }
        return items;
    }
}
