package com.todaycertificate.member;

import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.CurrentMember;
import com.todaycertificate.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private final MemberRepository members;

    public MeController(MemberRepository members) {
        this.members = members;
    }

    @GetMapping("/api/me")
    @Transactional(readOnly = true)
    MemberResponse me(Authentication authentication) {
        // 토큰은 유효하지만 회원이 삭제된 경우
        Member member = members.findById(CurrentMember.id(authentication))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        return MemberResponse.from(member);
    }
}
