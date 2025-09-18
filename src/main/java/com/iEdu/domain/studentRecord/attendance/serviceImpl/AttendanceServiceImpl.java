package com.iEdu.domain.studentRecord.attendance.serviceImpl;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.studentRecord.attendance.dto.req.AttendanceForm;
import com.iEdu.domain.studentRecord.attendance.dto.res.AttendanceDto;
import com.iEdu.domain.studentRecord.attendance.dto.res.PeriodAttendanceDto;
import com.iEdu.domain.studentRecord.attendance.entity.Attendance;
import com.iEdu.domain.studentRecord.attendance.entity.AttendancePage;
import com.iEdu.domain.studentRecord.attendance.entity.PeriodAttendance;
import com.iEdu.domain.studentRecord.attendance.repository.AttendanceRepository;
import com.iEdu.domain.studentRecord.attendance.service.AttendanceService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.utils.RoleValidator;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;
    private final RoleValidator roleValidator;
    private final CacheManager cacheManager;

    // 본인의 모든 출결 조회 [학생 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDto> getMyAllAttendance(Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // ROLE_STUDENT 아닌 경우 예외 처리
        roleValidator.validateStudentRole(loginUser);
        // 정렬 조건 추가: year(오름차순), semester(FIRST_SEMESTER 우선), date(오름차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.asc("year"), Sort.Order.asc("semester"), Sort.Order.asc("date"))
        );
        Page<Attendance> attendancePage = attendanceRepository.findByMemberId(loginUser.getId(), sortedPageable);
        return attendancePage.map(this::convertToAttendanceDto);
    }

    // 학생의 모든 출결 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDto> getAllAttendance(Long studentId, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateAccessToStudent(loginUser, studentId);
        // 정렬 조건 추가: year(오름차순), semester(FIRST_SEMESTER 우선), date(오름차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.asc("year"), Sort.Order.asc("semester"), Sort.Order.asc("date"))
        );
        Page<Attendance> attendancePage = attendanceRepository.findByMemberId(studentId, sortedPageable);
        return attendancePage.map(this::convertToAttendanceDto);
    }

    // (학년/학기/월)로 본인 출결 조회 [학생 권한]
    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "attendance",
            key = "'student:' + #loginUser.id + ':' + #year + ':' + #semester + ':' + (#month != null ? #month : 'all') + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public Page<AttendanceDto> getMyFilterAttendance(
            Integer year, Semester semester, Integer month, Pageable pageable, LoginUserDto loginUser
    ) {
        checkPageSize(pageable.getPageSize());
        // ROLE_STUDENT 아닌 경우 예외 처리
        roleValidator.validateStudentRole(loginUser);
        String semesterStr = semester.name();

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "date")
        );
        Page<Attendance> attendancePage =
                attendanceRepository.findFilteredAttendancesByMemberAndYearAndSemesterAndOptionalMonth(
                        loginUser.getId(), year, semesterStr, month, sortedPageable
                );
        return attendancePage.map(this::convertToAttendanceDto);
    }

    // (학년/학기/월)로 학생 출결 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "attendance",
            key = "'student:' + #studentId + ':' + #year + ':' + #semester + ':' + (#month != null ? #month : 'all') + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public Page<AttendanceDto> getFilterAttendance(
            Long studentId, Integer year, Semester semester, Integer month, Pageable pageable, LoginUserDto loginUser
    ) {
        checkPageSize(pageable.getPageSize());
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateAccessToStudent(loginUser, studentId);
        String semesterStr = semester.name();

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "date")
        );
        Page<Attendance> attendancePage =
                attendanceRepository.findFilteredAttendancesByMemberAndYearAndSemesterAndOptionalMonth(
                        studentId, year, semesterStr, month, sortedPageable
                );
        return attendancePage.map(this::convertToAttendanceDto);
    }

    // 학생 출결 생성 [선생님 권한]
    @Override
    @Transactional
    public void createAttendance(Long studentId, AttendanceForm attendanceForm, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Member student = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Attendance attendance = Attendance.builder()
                .member(student)
                .year(attendanceForm.getYear())
                .semester(attendanceForm.getSemester())
                .date(attendanceForm.getDate())
                .build();
        for (PeriodAttendance pa : attendanceForm.getPeriodAttendances()) {
            pa.setAttendance(attendance); // set parent
            attendance.getPeriodAttendances().add(pa);
        }
        attendanceRepository.save(attendance);
    }

    // 학생 출결 수정 [선생님 권한]
    @Override
    @Transactional
    public void updateAttendance(Long attendanceId, AttendanceForm attendanceForm, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ServiceException(ReturnCode.ATTENDANCE_NOT_FOUND));
        attendance.setYear(attendanceForm.getYear());
        attendance.setSemester(attendanceForm.getSemester());
        attendance.setDate(attendanceForm.getDate());
        attendance.getPeriodAttendances().clear();
        for (PeriodAttendance pa : attendanceForm.getPeriodAttendances()) {
            pa.setAttendance(attendance);
            attendance.getPeriodAttendances().add(pa);
        }
        attendanceRepository.save(attendance);
        // 캐시 무효화
        evictAttendanceCache(attendance.getMember().getId(), attendance.getYear(), attendance.getSemester());
    }

    // 학생 출결 삭제 [선생님 권한]
    @Override
    @Transactional
    public void deleteAttendance(Long attendanceId, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ServiceException(ReturnCode.ATTENDANCE_NOT_FOUND));
        attendanceRepository.delete(attendance);
        // 캐시 무효화
        evictAttendanceCache(attendance.getMember().getId(), attendance.getYear(), attendance.getSemester());
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = AttendancePage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // 캐시 무효화
    private void evictAttendanceCache(Long studentId, Integer year, Semester semester) {
        String cacheKey = "attendance:" + studentId + ":" + year + ":" + semester;
        cacheManager.getCache("attendance").evictIfPresent(cacheKey);
        log.debug("Attendance key evicted: {}", cacheKey);
    }

    // Attendance -> AttendanceDto 변환
    @Override
    public AttendanceDto convertToAttendanceDto(Attendance attendance) {
        List<PeriodAttendanceDto> periodDtos = attendance.getPeriodAttendances().stream()
                .filter(pa -> pa.getState() != PeriodAttendance.State.출석)  // 출석 아닌 경우만
                .map(pa -> new PeriodAttendanceDto(
                        pa.getAttendance().getId(),
                        pa.getState(),
                        pa.getPeriod()
                ))
                .collect(Collectors.toList());
        return AttendanceDto.builder()
                .id(attendance.getId())
                .studentId(attendance.getMember().getId())
                .year(attendance.getYear())
                .semester(attendance.getSemester())
                .date(attendance.getDate())
                .periodAttendanceDtos(periodDtos)
                .build();
    }
}
