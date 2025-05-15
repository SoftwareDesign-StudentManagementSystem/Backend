package com.iEdu.domain.studentRecord.attendance.controller;

import com.iEdu.domain.studentRecord.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/rest-api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "출석 API")
public class ApiV1AttendanceController {
    private final AttendanceService attendanceService;

    // 본인의 모든 출결 조회 [학생 권한]


    // 학생의 모든 출결 조회 [학부모/선생님 권한]


    // (학년/학기)로 본인 출결 조회 [학생 권한]


    // (학년/학기)로 학생 출결 조회 [학부모/선생님 권한]


    // 학생 출결 생성 [선생님 권한]


    // 학생 출결 수정 [선생님 권한]


    // 학생 출결 삭제 [선생님 권힌]
}
