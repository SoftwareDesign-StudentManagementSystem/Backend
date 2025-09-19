package com.iEdu.domain.studentRecord.report.service;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.studentRecord.report.dto.req.ReportRequest;
import com.iEdu.global.common.enums.Semester;

import java.util.List;

public interface ReportGenerator {
    byte[] generateReport(Member student, Integer year, Semester semester, ReportRequest reportRequest); // 단일 학생
    byte[] generateSingleTypeReport(List<Member> students, Integer year, Semester semester, ReportRequest reportRequest, String type);
    String getFileExtension();
}
