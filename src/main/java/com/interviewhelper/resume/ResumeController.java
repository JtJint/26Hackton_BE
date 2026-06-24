package com.interviewhelper.resume;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interviewhelper.resume.ResumeRequests.ResumeAnalysisRequest;
import com.interviewhelper.resume.ResumeRequests.TextResumeRequest;
import com.interviewhelper.resume.ResumeResponses.ResumeAnalysisResponse;
import com.interviewhelper.resume.ResumeResponses.ResumeResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Validated
@Tag(name = "Frontend - Resume", description = "프론트가 이력서 텍스트와 기본 면접 정보를 전달하는 API")
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

	private final ResumeService resumeService;

	public ResumeController(ResumeService resumeService) {
		this.resumeService = resumeService;
	}

	@Operation(
		summary = "이력서 텍스트 저장",
		description = "프론트에서 PDF/DOCX/TXT 파일을 텍스트로 추출한 뒤 jobRole, careerLevel, position, interviewType과 함께 전송합니다."
	)
	@PostMapping("/text")
	public ResumeResponse createTextResume(@Valid @org.springframework.web.bind.annotation.RequestBody TextResumeRequest request) {
		return ResumeResponse.from(resumeService.createFromText(
			request.jobRole(),
			request.careerLevel(),
			request.position(),
			request.interviewType(),
			request.resumeText()
		));
	}

	@Operation(
		summary = "이력서 분석 요청",
		description = "이력서 텍스트와 면접 기본 정보를 저장하고, AI 서버 또는 로컬 대체 구현을 통해 요약/추천 질문 토픽을 반환합니다."
	)
	@PostMapping("/analyze")
	public ResumeAnalysisResponse analyze(@Valid @org.springframework.web.bind.annotation.RequestBody ResumeAnalysisRequest request) {
		return resumeService.analyze(
			request.jobRole(),
			request.careerLevel(),
			request.position(),
			request.interviewType(),
			request.resumeText()
		);
	}
}
