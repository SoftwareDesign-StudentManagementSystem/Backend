package com.iEdu.domain.studentRecord.attendance.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/rest-api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "출석 API")
public class ApiV1AttendanceController {
}
