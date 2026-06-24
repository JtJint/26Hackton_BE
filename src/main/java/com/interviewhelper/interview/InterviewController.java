package com.interviewhelper.interview;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.interviewhelper.interview.InterviewRequests.CreateFeedbackRequest;
import com.interviewhelper.interview.InterviewRequests.CreateInterviewRequest;
import com.interviewhelper.interview.InterviewRequests.SubmitAnswerAnalysisRequest;
import com.interviewhelper.interview.InterviewRequests.SubmitAnswerRequest;
import com.interviewhelper.interview.InterviewResponses.AnswerResponse;
import com.interviewhelper.interview.InterviewResponses.FeedbackResponse;
import com.interviewhelper.interview.InterviewResponses.FeedbackResultResponse;
import com.interviewhelper.interview.InterviewResponses.InterviewResponse;
import com.interviewhelper.interview.InterviewResponses.QuestionResponse;
import com.interviewhelper.interview.InterviewResponses.QuestionsResponse;
import com.interviewhelper.interview.InterviewResponses.TranscribedAnswerResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Frontend - Interview", description = "프론트가 질문 생성, 답변 저장, 결과 리포트를 처리할 때 사용하는 API")
@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

	private final InterviewService interviewService;

	public InterviewController(InterviewService interviewService) {
		this.interviewService = interviewService;
	}

	@Operation(
		summary = "예상 질문 생성",
		description = "resumeId에 저장된 careerLevel, position, interviewType, resumeText를 기반으로 면접 질문을 생성합니다."
	)
	@PostMapping
	public InterviewResponse createInterview(@Valid @RequestBody CreateInterviewRequest request) {
		return InterviewResponse.from(interviewService.createInterview(request.resumeId(), request.questionCount()));
	}

	@Operation(summary = "질문 목록 조회", description = "생성된 면접 질문 목록을 interviewId로 조회합니다.")
	@GetMapping("/{interviewId}/questions")
	public QuestionsResponse getQuestions(@PathVariable Long interviewId) {
		return new QuestionsResponse(
			interviewId,
			interviewService.getQuestions(interviewId).stream().map(QuestionResponse::from).toList()
		);
	}

	@Operation(
		summary = "답변 텍스트 저장",
		description = "MVP용 API입니다. 프론트가 STT 또는 사용자가 입력한 답변 텍스트만 먼저 저장할 때 사용합니다."
	)
	@PostMapping("/{interviewId}/answers")
	public AnswerResponse submitAnswer(
		@PathVariable Long interviewId,
		@Valid @RequestBody SubmitAnswerRequest request
	) {
		return AnswerResponse.from(interviewService.submitAnswer(
			interviewId,
			request.questionId(),
			request.answerText(),
			request.durationSeconds()
		));
	}

	@Operation(
		summary = "분석 지표 포함 답변 저장",
		description = "프론트가 MediaPipe 시선 지표와 음성 분석 지표를 계산한 뒤 답변 텍스트와 함께 저장합니다."
	)
	@PostMapping("/{interviewId}/answers/analysis")
	public AnswerResponse submitAnswerAnalysis(
		@PathVariable Long interviewId,
		@Valid @RequestBody SubmitAnswerAnalysisRequest request
	) {
		return AnswerResponse.from(interviewService.submitAnswerAnalysis(
			interviewId,
			request.questionId(),
			request.answerText(),
			request.durationSeconds(),
			request.eyeAnalysis(),
			request.speechAnalysis()
		));
	}

	@Operation(
		summary = "음성 파일 답변 저장",
		description = "프론트가 녹음한 음성 파일과 MediaPipe 시선 지표를 보내면, 백엔드가 AI 서버 /transcribe로 STT/음성 분석을 요청하고 답변을 저장합니다."
	)
	@PostMapping(value = "/{interviewId}/answers/audio", consumes = "multipart/form-data")
	public TranscribedAnswerResponse submitAudioAnswer(
		@PathVariable Long interviewId,
		@RequestParam Long questionId,
		@RequestPart(required = false) MultipartFile audio,
		@RequestPart(required = false) MultipartFile file,
		@RequestParam(required = false) Double screenFocusRatio,
		@RequestParam(required = false) Integer gazeAwayCount,
		@RequestParam(required = false) Integer headMovementScore
	) {
		EyeAnalysis eyeAnalysis = new EyeAnalysis(screenFocusRatio, gazeAwayCount, headMovementScore);
		return TranscribedAnswerResponse.from(interviewService.submitAudioAnswer(
			interviewId,
			questionId,
			audio != null ? audio : file,
			eyeAnalysis
		));
	}

	@Operation(
		summary = "면접 피드백 생성",
		description = "저장된 답변들을 기반으로 내용/시선/음성 피드백과 추천 답변을 생성합니다."
	)
	@PostMapping("/{interviewId}/feedback")
	public FeedbackResponse createFeedback(
		@PathVariable Long interviewId,
		@RequestBody CreateFeedbackRequest request
	) {
		return FeedbackResponse.from(interviewService.createFeedback(
			interviewId,
			request.userId(),
			request.interviewerType(),
			request.answerIds()
		));
	}

	@Operation(summary = "면접 결과 조회", description = "생성된 최종 리포트와 질문별 피드백을 조회합니다.")
	@GetMapping("/{interviewId}/feedback")
	public FeedbackResultResponse getFeedback(@PathVariable Long interviewId) {
		return FeedbackResultResponse.from(interviewService.getFeedback(interviewId));
	}
}
