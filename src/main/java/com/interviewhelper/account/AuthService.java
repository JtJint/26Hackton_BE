package com.interviewhelper.account;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interviewhelper.common.BusinessException;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;

	public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
		this.userRepository = userRepository;
		this.passwordHasher = passwordHasher;
	}

	@Transactional
	public UserEntity signup(String email, String nickname, String password) {
		String normalizedEmail = normalizeEmail(email);
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new BusinessException("EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT);
		}

		UserEntity user = new UserEntity(normalizedEmail, nickname.trim(), passwordHasher.hash(password));
		return userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public UserEntity login(String email, String password) {
		UserEntity user = userRepository.findByEmail(normalizeEmail(email))
			.orElseThrow(() -> new BusinessException("LOGIN_FAILED", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED));

		if (!passwordHasher.matches(password, user.getPasswordHash())) {
			throw new BusinessException("LOGIN_FAILED", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
		}
		return user;
	}

	@Transactional(readOnly = true)
	public UserEntity getUser(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
	}

	private String normalizeEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase();
	}
}
