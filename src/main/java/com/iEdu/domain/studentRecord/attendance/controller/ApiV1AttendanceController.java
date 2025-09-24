package com.iEdu.domain.studentRecord.attendance.controller;

import com.iEdu.domain.account.auth.currentUser.CurrentUser;
import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.studentRecord.attendance.dto.req.AttendanceRequest;
import com.iEdu.domain.studentRecord.attendance.dto.req.AttendanceUpdateRequest;
import com.iEdu.domain.studentRecord.attendance.dto.res.AttendanceResponse;
import com.iEdu.domain.studentRecord.attendance.entity.AttendancePage;
import com.iEdu.domain.studentRecord.attendance.service.AttendanceService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/rest-api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "출석 API")
public class ApiV1AttendanceController {
    private final AttendanceService attendanceService;

    // 본인의 모든 출결 조회 [학생 권한]
    @Operation(summary = "본인의 모든 출결 조회 [학생 권한]")
    @GetMapping
    public ApiResponse<List<AttendanceResponse>> getMyAllAttendance(@ModelAttribute AttendancePage request, @CurrentUser CurrentUserDto currentUser){
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(attendanceService.getMyAllAttendance(pageable, currentUser));
    }

    // 학생의 모든 출결 조회 [학부모/선생님 권한]
    @Operation(summary = "학생의 모든 출결 조회 [학부모/선생님 권한]")
    @GetMapping("/{studentId}")
    public ApiResponse<List<AttendanceResponse>> getAllAttendance(@ModelAttribute AttendancePage request,
                                                                  @PathVariable("studentId") Long studentId,
                                                                  @CurrentUser CurrentUserDto currentUser){
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(attendanceService.getAllAttendance(studentId, pageable, currentUser));
    }

    // (학년/학기/월)로 본인 출결 조회 [학생 권한]
    @Operation(summary = "(학년/학기/월)로 본인 출결 조회 [학생 권한]")
    @GetMapping("/filter")
    public ApiResponse<List<AttendanceResponse>> getMyFilterAttendance(@ModelAttribute AttendancePage request,
                                                                       @RequestParam(value = "year") Integer year,
                                                                       @RequestParam(value = "semester") Semester semester,
                                                                       @RequestParam(value = "month", required = false) Integer month,
                                                                       @CurrentUser CurrentUserDto currentUser){
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(attendanceService.getMyFilterAttendance(year, semester, month, pageable, currentUser).getContent());
    }

    // (학년/학기/월)로 학생 출결 조회 [학부모/선생님 권한]
    @Operation(summary = "(학년/학기/월)로 학생 출결 조회 [학부모/선생님 권한]")
    @GetMapping("/filter/{studentId}")
    public ApiResponse<List<AttendanceResponse>> getFilterAttendance(@ModelAttribute AttendancePage request,
                                                                     @PathVariable("studentId") Long studentId,
                                                                     @RequestParam(value = "year") Integer year,
                                                                     @RequestParam(value = "semester") Semester semester,
                                                                     @RequestParam(value = "month", required = false) Integer month,
                                                                     @CurrentUser CurrentUserDto currentUser){
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(attendanceService.getFilterAttendance(studentId, year, semester, month, pageable, currentUser).getContent());
    }

    // 학생 출결 생성 [선생님 권한]
    @Operation(summary = "학생 출결 생성 [선생님 권한]")
    @PostMapping("/{studentId}")
    public ApiResponse<Void> createAttendance(@PathVariable("studentId") Long studentId,
                                                @RequestBody @Valid AttendanceRequest attendanceRequest,
                                                @CurrentUser CurrentUserDto currentUser){
        attendanceService.createAttendance(studentId, attendanceRequest, currentUser);
        return ApiResponse.success();
    }

    // 학생 출결 수정 [선생님 권한]
    @Operation(summary = "학생 출결 수정 [선생님 권한]")
    @PatchMapping("/{attendanceId}")
    public ApiResponse<Void> updateAttendance(@PathVariable("attendanceId") Long attendanceId,
                                                @RequestBody @Valid AttendanceUpdateRequest attendanceUpdateRequest,
                                                @CurrentUser CurrentUserDto currentUser){
        attendanceService.updateAttendance(attendanceId, attendanceUpdateRequest, currentUser);
        return ApiResponse.success();
    }

    // 학생 출결 삭제 [선생님 권힌]
    @Operation(summary = "학생 출결 삭제 [선생님 권힌]")
    @DeleteMapping("/{attendanceId}")
    public ApiResponse<Void> deleteAttendance(@PathVariable("attendanceId") Long attendanceId, @CurrentUser CurrentUserDto currentUser){
        attendanceService.deleteAttendance(attendanceId, currentUser);
        return ApiResponse.success();
    }
}
