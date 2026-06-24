package com.interviewhelper.account;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interviewhelper.account.AuthRequests.LoginRequest;
import com.interviewhelper.account.AuthRequests.SignupRequest;
import com.interviewhelper.account.AuthResponses.AuthResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Frontend - Auth", description = "로그인/회원가입 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@Operation(summary = "회원가입", description = "이메일, 닉네임, 비밀번호로 사용자를 생성합니다.")
	@PostMapping("/signup")
	public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
		return AuthResponse.from(authService.signup(request.email(), request.nickname(), request.password()));
	}

	@Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 userId를 반환합니다.")
	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return AuthResponse.from(authService.login(request.email(), request.password()));
	}

	@Operation(summary = "내 정보 조회", description = "MVP용 userId 기반 사용자 정보를 조회합니다.")
	@GetMapping("/users/{userId}")
	public AuthResponse getUser(@PathVariable Long userId) {
		return AuthResponse.from(authService.getUser(userId));
	}
}
