package com.interviewhelper.dashboard;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "practice_results")
public class PracticeResultEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long interviewId;

	@Column(nullable = false)
	private Integer totalScore;

	@Column(nullable = false)
	private Integer contentScore;

	@Column(nullable = false)
	private Integer eyeScore;

	@Column(nullable = false)
	private Integer speechScore;

	@Column(nullable = false, length = 1000)
	private String summary;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected PracticeResultEntity() {
	}

	public PracticeResultEntity(
		Long userId,
		Long interviewId,
		Integer totalScore,
		Integer contentScore,
		Integer eyeScore,
		Integer speechScore,
		String summary
	) {
		this.userId = userId;
		this.interviewId = interviewId;
		this.totalScore = totalScore;
		this.contentScore = contentScore;
		this.eyeScore = eyeScore;
		this.speechScore = speechScore;
		this.summary = summary;
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

	public Long getUserId() {
		return userId;
	}

	public Long getInterviewId() {
		return interviewId;
	}

	public Integer getTotalScore() {
		return totalScore;
	}

	public Integer getContentScore() {
		return contentScore;
	}

	public Integer getEyeScore() {
		return eyeScore;
	}

	public Integer getSpeechScore() {
		return speechScore;
	}

	public String getSummary() {
		return summary;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
