package com.iEdu.domain.studentRecord.attendance.serviceImpl;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
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
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.iEdu.global.common.utils.Converter.convertToSemesterEnum;
import static com.iEdu.global.common.utils.RoleValidator.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // 본인의 모든 출결 조회 [학생 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDto> getMyAllAttendance(Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(오름차순), semester(FIRST_SEMESTER 우선), date(오름차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.asc("year"), Sort.Order.asc("semester"), Sort.Order.asc("date"))
        );
        // ROLE_STUDENT 아닌 경우 예외 처리
        validateStudentRole(loginUser);
        Page<Attendance> attendancePage = attendanceRepository.findByMemberId(loginUser.getId(), sortedPageable);
        return attendancePage.map(this::convertToAttendanceDto);
    }

    // 학생의 모든 출결 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDto> getAllAttendance(Long studentId, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(오름차순), semester(FIRST_SEMESTER 우선), date(오름차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.asc("year"), Sort.Order.asc("semester"), Sort.Order.asc("date"))
        );
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        validateAccessToStudent(loginUser, studentId);
        Page<Attendance> attendancePage = attendanceRepository.findByMemberId(studentId, sortedPageable);
        return attendancePage.map(this::convertToAttendanceDto);
    }

    // (학년/학기/월)로 본인 출결 조회 [학생 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDto> getMyFilterAttendance(Integer year, Integer semester, Integer month, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // ROLE_STUDENT 아닌 경우 예외 처리
        validateStudentRole(loginUser);
        Semester semesterEnum = convertToSemesterEnum(semester);
        // 캐시 키 생성
        String cacheKey = buildCacheKey(loginUser.getId(), year, semesterEnum, month);
        // 캐시 조회
        Page<AttendanceDto> cachedResult = (Page<AttendanceDto>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        // 정렬 조건 추가: date(오름차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "date")
        );
        Page<Attendance> attendancePage = attendanceRepository
                .findFilteredAttendancesByMemberAndYearAndSemesterAndOptionalMonth(
                        loginUser.getId(), year, semesterEnum, month, sortedPageable
                );
        Page<AttendanceDto> resultPage = attendancePage.map(this::convertToAttendanceDto);
        // 캐시에 저장 (10분 TTL)
        redisTemplate.opsForValue().set(cacheKey, resultPage, Duration.ofMinutes(10));
        return resultPage;
    }

    // (학년/학기/월)로 학생 출결 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDto> getFilterAttendance(Long studentId, Integer year, Integer semester, Integer month, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        validateAccessToStudent(loginUser, studentId);
        Semester semesterEnum = convertToSemesterEnum(semester);

        // 캐시 키 생성 (학생ID, 연도, 학기, 월)
        String cacheKey = buildCacheKey(studentId, year, semesterEnum, month);

        // 캐시 조회
        Page<AttendanceDto> cachedResult = (Page<AttendanceDto>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "date")
        );
        Page<Attendance> attendancePage = attendanceRepository
                .findFilteredAttendancesByMemberAndYearAndSemesterAndOptionalMonth(
                        studentId, year, semesterEnum, month, sortedPageable
                );
        Page<AttendanceDto> resultPage = attendancePage.map(this::convertToAttendanceDto);
        // 캐시에 저장 (10분 TTL)
        redisTemplate.opsForValue().set(cacheKey, resultPage, Duration.ofMinutes(10));
        return resultPage;
    }

    // 학생 출결 생성 [선생님 권한]
    @Override
    @Transactional
    public void createAttendance(Long studentId, AttendanceForm attendanceForm, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
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
        validateTeacherRole(loginUser);
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
        evictAttendanceCache(attendance.getMember().getId());
    }

    // 학생 출결 삭제 [선생님 권한]
    @Override
    @Transactional
    public void deleteAttendance(Long attendanceId, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ServiceException(ReturnCode.ATTENDANCE_NOT_FOUND));
        attendanceRepository.delete(attendance);
        // 캐시 무효화
        evictAttendanceCache(attendance.getMember().getId());
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = AttendancePage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // 출결 캐시 생성
    private String buildCacheKey(Long studentId, Integer year, Semester semesterEnum, Integer month) {
        return "attendance:" + studentId + ":" + year + ":" + semesterEnum + ":" + (month == null ? "all" : month);
    }

    // 출결 캐시 삭제
    private void evictAttendanceCache(Long studentId) {
        // Redis 키 패턴: attendance:studentId:*
        String pattern = "attendance:" + studentId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
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
