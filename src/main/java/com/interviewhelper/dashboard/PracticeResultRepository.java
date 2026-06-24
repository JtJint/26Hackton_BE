package com.interviewhelper.dashboard;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeResultRepository extends JpaRepository<PracticeResultEntity, Long> {

	List<PracticeResultEntity> findByUserIdOrderByCreatedAtAsc(Long userId);

	List<PracticeResultEntity> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
}
