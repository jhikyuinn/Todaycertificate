package com.todaycertificate.solve;

import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.ErrorCode;
import com.todaycertificate.exam.Question;
import com.todaycertificate.exam.QuestionRepository;
import com.todaycertificate.member.MemberRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolveService {

    private final QuestionRepository questions;
    private final MemberRepository members;
    private final SolveRecordRepository records;

    public SolveService(QuestionRepository questions, MemberRepository members, SolveRecordRepository records) {
        this.questions = questions;
        this.members = members;
        this.records = records;
    }

    public record SolveResult(boolean correct, int answerNo, String explanation) {
    }

    /** 공부 모드: 한 문제를 풀면 바로 기록하고 정답과 해설을 돌려준다. */
    @Transactional
    public SolveResult solveInStudy(Long memberId, Long questionId, int selectedNo) {
        Question question = questions.findById(questionId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        // getReferenceById: 회원 SELECT 없이 외래키 값만 가진 프록시를 쓴다 (토큰으로 이미 존재가 확인된 회원)
        SolveRecord record = records.save(new SolveRecord(members.getReferenceById(memberId), question, null,
                selectedNo, SolveMode.STUDY, Instant.now()));
        return new SolveResult(record.isCorrect(), question.getAnswerNo(), question.getExplanation());
    }
}
