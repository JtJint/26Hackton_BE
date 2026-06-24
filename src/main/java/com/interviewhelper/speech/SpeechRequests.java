package com.interviewhelper.speech;

import com.interviewhelper.resume.InterviewerType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SpeechRequests {

	private SpeechRequests() {
	}

	@Schema(description = "질문 텍스트를 면접관 음성으로 변환하는 요청")
	public record SynthesizeSpeechRequest(
		@Schema(description = "음성으로 변환할 질문 또는 꼬리질문 텍스트", example = "Spring Boot를 사용한 이유는 무엇인가요?")
		@NotBlank @Size(max = 4096) String text,
		@Schema(description = "OpenAI TTS 보이스. 생략하면 nova", example = "nova")
		String voice,
		@Schema(description = "면접관 스타일. 생략하면 SOFT", example = "SOFT")
		InterviewerType interviewerType,
		@Schema(description = "말투/톤 지시. 생략하면 AI 서버 기본 면접관 톤 사용", example = "차분한 한국어 면접관처럼 말해 주세요.")
		String instructions
	) {
	}
}
