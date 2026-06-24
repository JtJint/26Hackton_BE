package com.interviewhelper.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interviewhelper.dashboard.DashboardResponses.DashboardResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Frontend - Dashboard", description = "사용자별 면접 성장 추적 API")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@Operation(summary = "대시보드 조회", description = "사용자별 면접 연습 횟수, 평균 점수, 성장 추이, 취약 영역, 최근 기록을 조회합니다.")
	@GetMapping("/{userId}")
	public DashboardResponse getDashboard(@PathVariable Long userId) {
		return dashboardService.getDashboard(userId);
	}
}
