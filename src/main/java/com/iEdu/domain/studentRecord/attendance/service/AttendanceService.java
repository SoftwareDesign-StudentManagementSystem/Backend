package com.iEdu.domain.studentRecord.attendance.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.attendance.dto.req.AttendanceRequest;
import com.iEdu.domain.studentRecord.attendance.dto.req.AttendanceUpdateRequest;
import com.iEdu.domain.studentRecord.attendance.dto.res.AttendanceResponse;
import com.iEdu.domain.studentRecord.attendance.entity.Attendance;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AttendanceService {
    // 본인의 모든 출결 조회 [학생 권한]
    PageResponse<AttendanceResponse> getMyAllAttendance(Pageable pageable, LoginUserDto loginUser);

    // 학생의 모든 출결 조회 [학부모/선생님 권한]
    PageResponse<AttendanceResponse> getAllAttendance(Long studentId, Pageable pageable, LoginUserDto loginUser);

    // (학년/학기/월)로 본인 출결 조회 [학생 권한]
    PageResponse<AttendanceResponse> getMyFilterAttendance(Integer year, Semester semester, Integer month, Pageable pageable, LoginUserDto loginUser);

    // (학년/학기/월)로 학생 출결 조회 [학부모/선생님 권한]
    PageResponse<AttendanceResponse>  getFilterAttendance(Long studentId, Integer year, Semester semester, Integer month, Pageable pageable, LoginUserDto loginUser);

    // 학생 출결 생성 [선생님 권한]
    void createAttendance(Long studentId, AttendanceRequest attendanceRequest, LoginUserDto loginUser);

    // 학생 출결 수정 [선생님 권한]
    void updateAttendance(Long attendanceId, AttendanceUpdateRequest attendanceUpdateRequest, LoginUserDto loginUser);

    // 학생 출결 삭제 [선생님 권힌]
    void deleteAttendance(Long attendanceId, LoginUserDto loginUser);

    // Attendance -> AttendanceDto 변환
    AttendanceResponse convertToAttendanceDto(Attendance attendance);
}
