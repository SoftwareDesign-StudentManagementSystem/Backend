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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // 본인의 모든 출결 조회 [학생 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDto> getMyAllAttendance(Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.asc("year"), Sort.Order.asc("semester"), Sort.Order.asc("date"))
        );
        // ROLE_STUDENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
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

    // (학년/학기)로 본인 출결 조회 [학생 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDto> getMyFilterAttendance(Integer year, Integer semester, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: date(오름차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "date")
        );
        // ROLE_STUDENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Semester semesterEnum = convertToSemesterEnum(semester);
        Page<Attendance> attendancePage = attendanceRepository
                .findAllByMemberIdAndYearAndSemester(loginUser.getId(), year, semesterEnum, sortedPageable);
        return attendancePage.map(this::convertToAttendanceDto);
    }

    // (학년/학기)로 학생 출결 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDto> getFilterAttendance(Long studentId, Integer year, Integer semester, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: date(오름차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "date")
        );
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        validateAccessToStudent(loginUser, studentId);
        Semester semesterEnum = convertToSemesterEnum(semester);
        Page<Attendance> attendancePage = attendanceRepository
                .findAllByMemberIdAndYearAndSemester(studentId, year, semesterEnum, pageable);
        return attendancePage.map(this::convertToAttendanceDto);
    }

    // 학생 출결 생성 [선생님 권한]
    @Override
    @Transactional
    public void createAttendance(Long studentId, AttendanceForm form, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member student = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Attendance attendance = Attendance.builder()
                .member(student)
                .year(form.getYear())
                .semester(form.getSemester())
                .date(form.getDate())
                .build();
        for (PeriodAttendance pa : form.getPeriodAttendances()) {
            pa.setAttendance(attendance); // set parent
            attendance.getPeriodAttendances().add(pa);
        }
        attendanceRepository.save(attendance);
    }

    // 학생 출결 수정 [선생님 권한]
    @Override
    @Transactional
    public void updateAttendance(Long attendanceId, AttendanceForm form, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ServiceException(ReturnCode.ATTENDANCE_NOT_FOUND));
        attendance.setYear(form.getYear());
        attendance.setSemester(form.getSemester());
        attendance.setDate(form.getDate());
        attendance.getPeriodAttendances().clear();
        for (PeriodAttendance pa : form.getPeriodAttendances()) {
            pa.setAttendance(attendance);
            attendance.getPeriodAttendances().add(pa);
        }
    }

    // 학생 출결 삭제 [선생님 권한]
    @Override
    @Transactional
    public void deleteAttendance(Long attendanceId, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ServiceException(ReturnCode.ATTENDANCE_NOT_FOUND));
        attendanceRepository.delete(attendance);
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = AttendancePage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
    private void validateAccessToStudent(LoginUserDto loginUser, Long studentId) {
        Member.MemberRole role = loginUser.getRole();
        if (role != Member.MemberRole.ROLE_PARENT && role != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        // ROLE_PARENT인 경우 자녀인지 확인
        if (role == Member.MemberRole.ROLE_PARENT) {
            Member parent = loginUser.ConvertToMember();
            boolean isMyChild = parent.getFollowList().stream()
                    .map(MemberFollow::getFollowed)
                    .anyMatch(child -> child != null && child.getId().equals(studentId));
            if (!isMyChild) {
                throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
            }
        }
    }

    // 학기 Integer -> Enum 변환
    private Semester convertToSemesterEnum(Integer semester) {
        return switch (semester) {
            case 1 -> Semester.FIRST_SEMESTER;
            case 2 -> Semester.SECOND_SEMESTER;
            default -> throw new ServiceException(ReturnCode.INVALID_SEMESTER);
        };
    }

    // Attendance -> AttendanceDto 변환
    private AttendanceDto convertToAttendanceDto(Attendance attendance) {
        List<PeriodAttendanceDto> periodDtos = attendance.getPeriodAttendances().stream()
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

