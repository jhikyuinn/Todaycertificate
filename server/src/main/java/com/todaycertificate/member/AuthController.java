package com.todaycertificate.member;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    record SignupRequest(
            @NotNull @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "아이디는 영문과 숫자 4~20자입니다.") String loginId,
            @NotBlank @Size(max = 20) String name,
            @NotNull @Size(min = 8, max = 64, message = "비밀번호는 8~64자입니다.") String password) {
    }

    record SignupResponse(Long id) {
    }

    record LoginRequest(@NotBlank String loginId, @NotBlank String password) {
    }

    record LoginResponse(String accessToken, MemberResponse member) {
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        return new SignupResponse(authService.signup(request.loginId(), request.name(), request.password()));
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.loginId(), request.password());
        return new LoginResponse(result.accessToken(), MemberResponse.from(result.member()));
    }
}
