package com.interviewhelper.account;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

	boolean existsByEmail(String email);

	Optional<UserEntity> findByEmail(String email);
}
