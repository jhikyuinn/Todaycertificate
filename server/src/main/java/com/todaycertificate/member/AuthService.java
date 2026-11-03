package com.todaycertificate.member;

import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration tokenLifetime;

    public AuthService(MemberRepository members, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       @Value("${app.jwt.expiry-hours}") long expiryHours) {
        this.members = members;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.tokenLifetime = Duration.ofHours(expiryHours);
    }

    @Transactional
    public Long signup(String loginId, String name, String rawPassword) {
        if (members.existsByLoginId(loginId)) {
            throw new ApiException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        try {
            Member member = new Member(loginId, name, passwordEncoder.encode(rawPassword));
            return members.saveAndFlush(member).getId();
        } catch (DataIntegrityViolationException e) {
            // 위 확인과 저장 사이에 같은 아이디로 동시에 가입한 경우: DB UNIQUE 제약이 막아준다
            throw new ApiException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
    }

    @Transactional(readOnly = true)
    public LoginResult login(String loginId, String rawPassword) {
        // 아이디가 없을 때와 비밀번호가 틀릴 때 같은 오류를 낸다: 가입된 아이디를 알려주지 않기 위해
        Member member = members.findByLoginId(loginId)
                .filter(m -> passwordEncoder.matches(rawPassword, m.getPassword()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        return new LoginResult(issueToken(member), member);
    }

    private String issueToken(Member member) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(member.getId().toString())
                .claim("role", member.getRole().name())
                .issuedAt(now)
                .expiresAt(now.plus(tokenLifetime))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public record LoginResult(String accessToken, Member member) {
    }
}
