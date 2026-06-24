package com.interviewhelper.resume;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interviewhelper.resume.ResumeRequests.ResumeAnalysisRequest;
import com.interviewhelper.resume.ResumeRequests.TextResumeRequest;
import com.interviewhelper.resume.ResumeResponses.ResumeAnalysisResponse;
import com.interviewhelper.resume.ResumeResponses.ResumeResponse;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

	private final ResumeService resumeService;

	public ResumeController(ResumeService resumeService) {
		this.resumeService = resumeService;
	}

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
