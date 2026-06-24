package com.interviewhelper.account;

import java.time.LocalDateTime;

public final class AuthResponses {

	private AuthResponses() {
	}

	public record AuthResponse(
		Long userId,
		String email,
		String nickname,
		LocalDateTime createdAt
	) {
		public static AuthResponse from(UserEntity user) {
			return new AuthResponse(user.getId(), user.getEmail(), user.getNickname(), user.getCreatedAt());
		}
	}
}
