package com.iEdu.domain.studentRecord.report.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.report.dto.req.ReportForm;
import com.iEdu.domain.studentRecord.report.dto.res.ReportDto;

public interface ReportService {
    // 학생 보고서 생성 및 다운로드 [선생님 권한]
    ReportDto generateReport(ReportForm reportForm, LoginUserDto loginUser);
}
