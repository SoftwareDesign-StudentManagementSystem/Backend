package com.iEdu.domain.studentRecord.report.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.report.dto.req.ReportForm;
import com.iEdu.domain.studentRecord.report.dto.res.ReportDto;
import com.iEdu.domain.studentRecord.report.service.ReportService;
import com.iEdu.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "학생 보고서 생성 및 다운로드 [선생님 권한]")
    @PostMapping
    public ApiResponse<ReportDto> generateReport(@RequestBody @Valid ReportForm reportForm, @LoginUser LoginUserDto loginUser) {
        return ApiResponse.of(reportService.generateReport(reportForm, loginUser));
    }
}
