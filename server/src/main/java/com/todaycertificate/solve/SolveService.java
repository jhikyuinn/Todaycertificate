package com.todaycertificate.solve;

import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.ErrorCode;
import com.todaycertificate.exam.Question;
import com.todaycertificate.exam.QuestionRepository;
import com.todaycertificate.member.MemberRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolveService {

    private final QuestionRepository questions;
    private final MemberRepository members;
    private final SolveRecordRepository records;
    private final QuestionStatsService stats;

    public SolveService(QuestionRepository questions, MemberRepository members, SolveRecordRepository records,
                        QuestionStatsService stats) {
        this.questions = questions;
        this.members = members;
        this.records = records;
        this.stats = stats;
    }

    /**
     * 객관식은 answerNo, 주관식은 correctAnswerText('|'로 구분한 허용 답 원문)만 채운다.
     * stats: 방금 푼 기록까지 포함한 전체 회원 통계. 표본이 적으면 null.
     */
    public record SolveResult(boolean correct, Integer answerNo, String correctAnswerText, String explanation,
                              QuestionStatsService.Stats stats) {
    }

    /** 공부 모드: 한 문제를 풀면 바로 기록하고 정답과 해설을 돌려준다. */
    @Transactional
    public SolveResult solveInStudy(Long memberId, Long questionId, Integer selectedNo, String answerText) {
        Question question = questions.findById(questionId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        String invalid = question.invalidAnswerField(selectedNo, answerText);
        if (invalid != null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, List.of(Map.of("field", invalid,
                    "message", "객관식은 selectedNo, 주관식은 answerText 하나만 보내야 합니다")));
        }
        // getReferenceById: 회원 SELECT 없이 외래키 값만 가진 프록시를 쓴다 (토큰으로 이미 존재가 확인된 회원)
        SolveRecord record = records.save(new SolveRecord(members.getReferenceById(memberId), question, null,
                selectedNo, answerText, SolveMode.STUDY, Instant.now()));
        return new SolveResult(record.isCorrect(), question.getAnswerNo(), question.getAnswerText(),
                question.getExplanation(),
                stats.statsFor(List.of(question)).get(questionId));
    }
}
