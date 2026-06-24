package com.interviewhelper.account;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String email;

	@Column(nullable = false, length = 60)
	private String nickname;

	@Column(nullable = false, length = 512)
	private String passwordHash;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected UserEntity() {
	}

	public UserEntity(String email, String nickname, String passwordHash) {
		this.email = email;
		this.nickname = nickname;
		this.passwordHash = passwordHash;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getNickname() {
		return nickname;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
