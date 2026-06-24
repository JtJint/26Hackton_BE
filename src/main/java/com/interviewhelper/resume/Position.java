package com.interviewhelper.resume;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원 포지션")
public enum Position {
	BACKEND,
	FRONTEND,
	FULLSTACK,
	AI,
	DEVOPS,
	MOBILE,
	ETC
}
