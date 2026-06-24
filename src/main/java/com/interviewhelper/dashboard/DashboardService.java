package com.interviewhelper.dashboard;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewhelper.dashboard.DashboardResponses.AreaScores;
import com.interviewhelper.dashboard.DashboardResponses.DashboardResponse;
import com.interviewhelper.dashboard.DashboardResponses.RecentPracticeResponse;

@Service
public class DashboardService {

	private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
	private static final TypeReference<List<QuestionLog>> QUESTION_LOG_LIST_TYPE = new TypeReference<>() {
	};

	private final PracticeResultRepository practiceResultRepository;
	private final ObjectMapper objectMapper;

	public DashboardService(PracticeResultRepository practiceResultRepository) {
		this.practiceResultRepository = practiceResultRepository;
		this.objectMapper = new ObjectMapper().findAndRegisterModules();
	}

	@Transactional
	public void recordPracticeResult(
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
		String followUpQuestion,
		List<QuestionLog> questionLogs
	) {
		if (userId == null) {
			log.warn("Dashboard practice result skipped because userId is null. interviewId={}", interviewId);
			return;
		}
		PracticeResultEntity result = practiceResultRepository.save(new PracticeResultEntity(
			userId,
			interviewId,
			valueOrZero(totalScore),
			valueOrZero(contentScore),
			valueOrZero(eyeScore),
			valueOrZero(speechScore),
			normalize(summary, "면접 피드백이 생성되었습니다."),
			normalize(contentStrength, ""),
			normalize(contentImprovement, ""),
			normalize(eyeStrength, ""),
			normalize(eyeImprovement, ""),
			normalize(speechStrength, ""),
			normalize(speechImprovement, ""),
			normalize(recommendedAnswer, ""),
			normalize(gapCriterion, ""),
			normalize(followUpQuestion, ""),
			toQuestionLogsJson(questionLogs)
		));
		log.info(
			"Dashboard practice result saved. userId={}, interviewId={}, resultId={}, totalScore={}",
			userId,
			interviewId,
			result.getId(),
			result.getTotalScore()
		);
	}

	@Transactional(readOnly = true)
	public DashboardResponse getDashboard(Long userId) {
		List<PracticeResultEntity> results = practiceResultRepository.findByUserIdOrderByCreatedAtAsc(userId);
		List<RecentPracticeResponse> recentPractices = practiceResultRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId)
			.stream()
			.map(this::toRecentPracticeResponse)
			.toList();

		if (results.isEmpty()) {
			return new DashboardResponse(
				userId,
				0,
				0,
				0,
				"none",
				"아직 연습 기록 없음",
				"첫 모의면접을 완료하면 취약 영역과 추천 연습이 표시됩니다.",
				new AreaScores(0, 0, 0),
				recentPractices
			);
		}

		int averageScore = average(results.stream().map(PracticeResultEntity::getTotalScore).toList());
		int contentAverage = average(results.stream().map(PracticeResultEntity::getContentScore).toList());
		int eyeAverage = average(results.stream().map(PracticeResultEntity::getEyeScore).toList());
		int speechAverage = average(results.stream().map(PracticeResultEntity::getSpeechScore).toList());
		String weakestArea = weakestArea(contentAverage, eyeAverage, speechAverage);
		int scoreTrend = scoreTrend(results);

		return new DashboardResponse(
			userId,
			results.size(),
			averageScore,
			scoreTrend,
			weakestArea,
			areaLabel(weakestArea),
			recommendPractice(weakestArea),
			new AreaScores(contentAverage, eyeAverage, speechAverage),
			recentPractices.stream()
				.sorted(Comparator.comparing(RecentPracticeResponse::createdAt).reversed())
				.toList()
		);
	}

	private int scoreTrend(List<PracticeResultEntity> results) {
		if (results.size() < 2) {
			return 0;
		}
		PracticeResultEntity latest = results.get(results.size() - 1);
		PracticeResultEntity previous = results.get(results.size() - 2);
		return latest.getTotalScore() - previous.getTotalScore();
	}

	private int average(List<Integer> values) {
		return Math.round((float) values.stream().mapToInt(this::valueOrZero).average().orElse(0));
	}

	private String weakestArea(int content, int eye, int speech) {
		if (content <= eye && content <= speech) {
			return "content";
		}
		if (speech <= content && speech <= eye) {
			return "speech";
		}
		return "eye";
	}

	private String areaLabel(String area) {
		return switch (area) {
			case "content" -> "답변 내용";
			case "speech" -> "발화";
			case "eye" -> "시선";
			default -> "아직 연습 기록 없음";
		};
	}

	private String recommendPractice(String area) {
		return switch (area) {
			case "content" -> "프로젝트 경험을 문제 상황, 본인 역할, 해결 과정, 결과 순서로 말하는 연습을 추천합니다.";
			case "speech" -> "30초 답변을 녹음하며 말 속도와 습관어를 줄이는 연습을 추천합니다.";
			case "eye" -> "핵심 문장을 말할 때 화면을 바라보고 고개 움직임을 줄이는 연습을 추천합니다.";
			default -> "첫 모의면접을 완료해 주세요.";
		};
	}

	private int valueOrZero(Integer value) {
		return value == null ? 0 : value;
	}

	private String normalize(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value;
	}

	private RecentPracticeResponse toRecentPracticeResponse(PracticeResultEntity result) {
		return RecentPracticeResponse.from(result, fromQuestionLogsJson(result.getQuestionLogsJson()));
	}

	private String toQuestionLogsJson(List<QuestionLog> questionLogs) {
		try {
			return objectMapper.writeValueAsString(questionLogs == null ? List.of() : questionLogs);
		}
		catch (JsonProcessingException exception) {
			log.warn("Question logs serialization failed. size={}", questionLogs == null ? 0 : questionLogs.size(), exception);
			return "[]";
		}
	}

	private List<QuestionLog> fromQuestionLogsJson(String questionLogsJson) {
		if (questionLogsJson == null || questionLogsJson.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(questionLogsJson, QUESTION_LOG_LIST_TYPE);
		}
		catch (JsonProcessingException exception) {
			log.warn("Question logs deserialization failed.", exception);
			return List.of();
		}
	}
}
