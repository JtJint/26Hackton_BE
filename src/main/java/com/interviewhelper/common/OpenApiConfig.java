package com.interviewhelper.common;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("Interview Helper Backend API")
				.description("프론트엔드가 사용하는 이력서 기반 모의면접 백엔드 API입니다.")
				.version("1.0.0"));
	}

	@Bean
	public GroupedOpenApi frontendApi() {
		return GroupedOpenApi.builder()
			.group("frontend-api")
			.pathsToMatch("/api/auth/**", "/api/dashboard/**", "/api/resumes/**", "/api/interviews/**", "/api/speech/**")
			.build();
	}
}
