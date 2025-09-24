package com.iEdu.domain.studentRecord.report.service;

import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.studentRecord.report.dto.req.ReportRequest;
import com.iEdu.domain.studentRecord.report.dto.res.ReportResponse;

public interface ReportService {
    // 학생 보고서 생성 및 다운로드 [선생님 권한]
    ReportResponse generateReport(ReportRequest reportRequest, CurrentUserDto currentUser);
}
