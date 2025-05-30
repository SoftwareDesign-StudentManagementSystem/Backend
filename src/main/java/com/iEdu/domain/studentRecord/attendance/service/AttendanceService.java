package com.iEdu.domain.studentRecord.attendance.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.attendance.dto.req.AttendanceForm;
import com.iEdu.domain.studentRecord.attendance.dto.res.AttendanceDto;
import com.iEdu.domain.studentRecord.attendance.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AttendanceService {
    // 본인의 모든 출결 조회 [학생 권한]
    Page<AttendanceDto> getMyAllAttendance(Pageable pageable, LoginUserDto loginUser);

    // 학생의 모든 출결 조회 [학부모/선생님 권한]
    Page<AttendanceDto> getAllAttendance(Long studentId, Pageable pageable, LoginUserDto loginUser);

    // (학년/학기/월)로 본인 출결 조회 [학생 권한]
    Page<AttendanceDto>  getMyFilterAttendance(Integer year, Integer semester, Integer month, Pageable pageable, LoginUserDto loginUser);

    // (학년/학기/월)로 학생 출결 조회 [학부모/선생님 권한]
    Page<AttendanceDto>  getFilterAttendance(Long studentId, Integer year, Integer semester, Integer month, Pageable pageable, LoginUserDto loginUser);

    // 학생 출결 생성 [선생님 권한]
    void createAttendance(Long studentId, AttendanceForm attendanceForm, LoginUserDto loginUser);

    // 학생 출결 수정 [선생님 권한]
    void updateAttendance(Long attendanceId, AttendanceForm attendanceForm, LoginUserDto loginUser);

    // 학생 출결 삭제 [선생님 권힌]
    void deleteAttendance(Long attendanceId, LoginUserDto loginUser);

    // Attendance -> AttendanceDto 변환
    AttendanceDto convertToAttendanceDto(Attendance attendance);
}
