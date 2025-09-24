package com.iEdu.domain.studentRecord.report.controller;

import com.iEdu.domain.account.auth.currentUser.CurrentUser;
import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.studentRecord.report.dto.req.ReportRequest;
import com.iEdu.domain.studentRecord.report.dto.res.ReportResponse;
import com.iEdu.domain.studentRecord.report.service.ReportService;
import com.iEdu.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/rest-api/v1/report")
@RequiredArgsConstructor
@Tag(name = "Report", description = "보고서 API")
public class ApiV1ReportController {
    private final ReportService reportService;

    // 학생 보고서 생성 및 다운로드 [선생님 권한]
    @Operation(summary = "학생 보고서 생성 및 다운로드 [선생님 권한]", description = "최대 25명까지 선택 가능")
    @PostMapping
    public ApiResponse<ReportResponse> generateReport(@RequestBody @Valid ReportRequest reportRequest, @CurrentUser CurrentUserDto currentUser) {
        return ApiResponse.success(reportService.generateReport(reportRequest, currentUser));
    }
}
