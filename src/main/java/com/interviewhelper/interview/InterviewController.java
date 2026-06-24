package com.interviewhelper.interview;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

	private final InterviewService interviewService;

	public InterviewController(InterviewService interviewService) {
		this.interviewService = interviewService;
	}

	@PostMapping
	public InterviewResponse createInterview(@Valid @RequestBody CreateInterviewRequest request) {
		return InterviewResponse.from(interviewService.createInterview(request.resumeId(), request.questionCount()));
	}

	@GetMapping("/{interviewId}/questions")
	public QuestionsResponse getQuestions(@PathVariable Long interviewId) {
		return new QuestionsResponse(
			interviewId,
			interviewService.getQuestions(interviewId).stream().map(QuestionResponse::from).toList()
		);
	}

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

	@PostMapping("/{interviewId}/feedback")
	public FeedbackResponse createFeedback(
		@PathVariable Long interviewId,
		@RequestBody CreateFeedbackRequest request
	) {
		return FeedbackResponse.from(interviewService.createFeedback(interviewId, request.answerIds()));
	}

	@GetMapping("/{interviewId}/feedback")
	public FeedbackResultResponse getFeedback(@PathVariable Long interviewId) {
		return FeedbackResultResponse.from(interviewService.getFeedback(interviewId));
	}
}
