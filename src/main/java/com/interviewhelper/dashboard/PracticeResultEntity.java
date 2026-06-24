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

	@Column(length = 1000)
	private String contentStrength;

	@Column(length = 1000)
	private String contentImprovement;

	@Column(length = 1000)
	private String eyeStrength;

	@Column(length = 1000)
	private String eyeImprovement;

	@Column(length = 1000)
	private String speechStrength;

	@Column(length = 1000)
	private String speechImprovement;

	@Column(length = 4000)
	private String recommendedAnswer;

	@Column(length = 100)
	private String gapCriterion;

	@Column(length = 1000)
	private String followUpQuestion;

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
		String summary,
		String contentStrength,
		String contentImprovement,
		String eyeStrength,
		String eyeImprovement,
		String speechStrength,
		String speechImprovement,
		String recommendedAnswer,
		String gapCriterion,
		String followUpQuestion
	) {
		this.userId = userId;
		this.interviewId = interviewId;
		this.totalScore = totalScore;
		this.contentScore = contentScore;
		this.eyeScore = eyeScore;
		this.speechScore = speechScore;
		this.summary = summary;
		this.contentStrength = contentStrength;
		this.contentImprovement = contentImprovement;
		this.eyeStrength = eyeStrength;
		this.eyeImprovement = eyeImprovement;
		this.speechStrength = speechStrength;
		this.speechImprovement = speechImprovement;
		this.recommendedAnswer = recommendedAnswer;
		this.gapCriterion = gapCriterion;
		this.followUpQuestion = followUpQuestion;
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

	public String getContentStrength() {
		return contentStrength;
	}

	public String getContentImprovement() {
		return contentImprovement;
	}

	public String getEyeStrength() {
		return eyeStrength;
	}

	public String getEyeImprovement() {
		return eyeImprovement;
	}

	public String getSpeechStrength() {
		return speechStrength;
	}

	public String getSpeechImprovement() {
		return speechImprovement;
	}

	public String getRecommendedAnswer() {
		return recommendedAnswer;
	}

	public String getGapCriterion() {
		return gapCriterion;
	}

	public String getFollowUpQuestion() {
		return followUpQuestion;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
