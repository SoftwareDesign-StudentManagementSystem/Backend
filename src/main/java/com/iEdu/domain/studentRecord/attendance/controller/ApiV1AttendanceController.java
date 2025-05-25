package com.iEdu.domain.studentRecord.attendance.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.attendance.dto.req.AttendanceForm;
import com.iEdu.domain.studentRecord.attendance.dto.res.AttendanceDto;
import com.iEdu.domain.studentRecord.attendance.entity.AttendancePage;
import com.iEdu.domain.studentRecord.attendance.service.AttendanceService;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.global.common.response.IEduPage;
import com.iEdu.global.exception.ReturnCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.shaded.com.google.protobuf.Api;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/rest-api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "출석 API")
public class ApiV1AttendanceController {
    private final AttendanceService attendanceService;

    // 본인의 모든 출결 조회 [학생 권한]
    @Operation(summary = "본인의 모든 출결 조회 [학생 권한]")
    @GetMapping
    public ApiResponse<AttendanceDto> getMyAllAttendance(@ModelAttribute AttendancePage request, @LoginUser LoginUserDto loginUser){
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(attendanceService.getMyAllAttendance(pageable, loginUser)));
    }

    // 학생의 모든 출결 조회 [학부모/선생님 권한]
    @Operation(summary = "학생의 모든 출결 조회 [학부모/선생님 권한]")
    @GetMapping("/{studentId}")
    public ApiResponse<AttendanceDto> getAllAttendance(@ModelAttribute AttendancePage request,
                                                       @PathVariable("studentId") Long studentId,
                                                       @LoginUser LoginUserDto loginUser){
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(attendanceService.getAllAttendance(studentId, pageable, loginUser)));
    }

    // (학년/학기)로 본인 출결 조회 [학생 권한]
    @Operation(summary = "(학년/학기)로 본인 출결 조회 [학생 권한]")
    @GetMapping("/filter")
    public ApiResponse<AttendanceDto> getMyFilterAttendance(@RequestParam(value = "year") Integer year,
                                                            @RequestParam(value = "semester") Integer semester,
                                                            @LoginUser LoginUserDto loginUser){
        return ApiResponse.of(attendanceService.getMyFilterAttendance(year, semester, loginUser));
    }

    // (학년/학기)로 학생 출결 조회 [학부모/선생님 권한]
    @Operation(summary = "(학년/학기)로 학생 출결 조회 [학부모/선생님 권한]")
    @GetMapping("/filter/{studentId}")
    public ApiResponse<AttendanceDto> getFilterAttendance(@PathVariable("studentId") Long studentId,
                                                          @RequestParam(value = "year") Integer year,
                                                          @RequestParam(value = "semester") Integer semester,
                                                          @LoginUser LoginUserDto loginUser){
        return ApiResponse.of(attendanceService.getFilterAttendance(studentId, year, semester, loginUser));
    }

    // 학생 출결 생성 [선생님 권한]
    @Operation(summary = "학생 출결 생성 [선생님 권한]")
    @PostMapping("/{studentId}")
    public ApiResponse<String> createAttendance(@PathVariable("studentId") Long studentId,
                                                @RequestBody @Valid AttendanceForm attendanceForm,
                                                @LoginUser LoginUserDto loginUser){
        attendanceService.createAttendance(studentId, attendanceForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 학생 출결 수정 [선생님 권한]
    @Operation(summary = "학생 출결 수정 [선생님 권한]")
    @PutMapping("/{attendanceId}")
    public ApiResponse<String> updateAttendance(@PathVariable("attendanceId") Long attendanceId,
                                                @RequestBody @Valid AttendanceForm attendanceForm,
                                                @LoginUser LoginUserDto loginUser){
        attendanceService.updateAttendance(attendanceId, attendanceForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 학생 출결 삭제 [선생님 권힌]
    @Operation(summary = "학생 출결 삭제 [선생님 권힌]")
    @DeleteMapping("/{attendanceId}")
    public ApiResponse<String> deleteAttendance(@PathVariable("attendanceId") Long attendanceId, @LoginUser LoginUserDto loginUser){
        attendanceService.deleteAttendance(attendanceId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }
}
