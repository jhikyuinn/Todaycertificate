package com.todaycertificate.common;

import org.springframework.security.core.Authentication;

/** 컨트롤러에서 현재 로그인한 회원의 ID를 꺼낸다. JWT의 sub(subject)에 회원 ID가 들어 있다. */
public final class CurrentMember {

    private CurrentMember() {
    }

    public static Long id(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
