package com.todaycertificate.member;

/** 회원 정보 응답. 비밀번호 해시는 절대 포함하지 않는다. */
public record MemberResponse(Long id, String loginId, String name, Role role) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getLoginId(), member.getName(), member.getRole());
    }
}
