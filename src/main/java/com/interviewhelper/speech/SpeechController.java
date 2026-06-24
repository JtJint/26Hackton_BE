package com.interviewhelper.speech;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interviewhelper.speech.SpeechRequests.SynthesizeSpeechRequest;
import com.interviewhelper.speech.SpeechResponses.SynthesizeSpeechResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Frontend - Speech", description = "프론트가 질문 TTS 음성을 생성할 때 사용하는 API")
@RestController
@RequestMapping("/api/speech")
public class SpeechController {

	private final SpeechService speechService;

	public SpeechController(SpeechService speechService) {
		this.speechService = speechService;
	}

	@Operation(
		summary = "질문 TTS 음성 생성",
		description = "질문 또는 꼬리질문 텍스트를 AI 서버 /synthesize로 전달해 면접관 음성 MP3 base64를 생성합니다."
	)
	@PostMapping("/synthesize")
	public SynthesizeSpeechResponse synthesize(@Valid @RequestBody SynthesizeSpeechRequest request) {
		return SynthesizeSpeechResponse.from(speechService.synthesize(
			request.text(),
			request.voice(),
			request.interviewerType(),
			request.instructions()
		));
	}
}
