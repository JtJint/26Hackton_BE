package com.interviewhelper.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequests {

	private AuthRequests() {
	}

	public record SignupRequest(
		@Schema(example = "min@example.com")
		@Email @NotBlank String email,
		@Schema(example = "민")
		@NotBlank @Size(max = 60) String nickname,
		@Schema(example = "password1234")
		@NotBlank @Size(min = 6, max = 80) String password
	) {
	}

	public record LoginRequest(
		@Schema(example = "min@example.com")
		@Email @NotBlank String email,
		@Schema(example = "password1234")
		@NotBlank String password
	) {
	}
}
