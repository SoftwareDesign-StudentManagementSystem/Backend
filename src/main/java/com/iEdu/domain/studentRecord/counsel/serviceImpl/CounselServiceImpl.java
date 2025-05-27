package com.iEdu.domain.studentRecord.counsel.serviceImpl;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.studentRecord.counsel.dto.req.CounselForm;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselDto;
import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import com.iEdu.domain.studentRecord.counsel.entity.CounselPage;
import com.iEdu.domain.studentRecord.counsel.repository.CounselQueryRepository;
import com.iEdu.domain.studentRecord.counsel.repository.CounselRepository;
import com.iEdu.domain.studentRecord.counsel.service.CounselService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.iEdu.global.common.utils.Converter.convertToSemesterEnum;
import static com.iEdu.global.common.utils.RoleValidator.*;

@Service
@RequiredArgsConstructor
public class CounselServiceImpl implements CounselService {
    private final CounselRepository counselRepository;
    private final MemberRepository memberRepository;
    private final CounselQueryRepository counselQueryRepository;

    // 본인의 모든 상담 조회 [학생 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<CounselDto> getMyAllCounsel(Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선), createdAt(내림차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"), Sort.Order.desc("createdAt"))
        );
        // ROLE_STUDENT 아닌 경우 예외 처리
        validateStudentRole(loginUser);
        Page<Counsel> counselPage = counselRepository.findByMemberId(loginUser.getId(), sortedPageable);
        return counselPage.map(this::convertToCounselDto);
    }

    // 학생의 모든 상담 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<CounselDto> getAllCounsel(Long studentId, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선), createdAt(내림차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"), Sort.Order.desc("createdAt"))
        );
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        validateAccessToStudent(loginUser, studentId);
        Page<Counsel> counselPage = counselRepository.findByMemberId(studentId, sortedPageable);
        return counselPage.map(this::convertToCounselDto);
    }

    // (학년/학기)로 본인 상담 조회 [학생 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<CounselDto> getMyFilterCounsel(Integer year, Integer semester, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // 정렬 조건: createdAt 내림차순
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        // ROLE_STUDENT 아닌 경우 예외 처리
        validateStudentRole(loginUser);
        Semester semesterEnum = convertToSemesterEnum(semester);
        Page<Counsel> counselPage = counselRepository.findByMemberIdAndYearAndSemester(
                loginUser.getId(), year, semesterEnum, sortedPageable
        );
        return counselPage.map(this::convertToCounselDto);
    }

    // (학년/반/번호/학기)로 학생들 상담 조회 [선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public List<CounselDto> getStudentsCounsel(Integer year, Integer classId, Integer number, Integer semester, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Semester semesterEnum = convertToSemesterEnum(semester);
        // 학생 목록 조회
        List<Member> students = memberRepository.findStudentsByYearClassNumber(year, classId, number);
        return students.stream()
                .flatMap(student -> counselQueryRepository
                        .findByMemberIdAndYearAndSemester(student.getId(), year, semesterEnum)
                        .stream()
                )
                .map(this::convertToCounselDto)
                .collect(Collectors.toList());
    }

    // (학년/학기)로 학생 상담 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<CounselDto> getFilterCounsel(Long studentId, Integer year, Integer semester, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // 정렬: year 내림차순, semester(SECOND_SEMESTER 우선), createdAt 내림차순
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"))
        );
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        validateAccessToStudent(loginUser, studentId);
        Semester semesterEnum = convertToSemesterEnum(semester);
        Page<Counsel> counselPage = counselRepository.findByMemberIdAndYearAndSemester(studentId, year, semesterEnum, sortedPageable);
        return counselPage.map(this::convertToCounselDto);
    }

    // 학생 상담 생성 [선생님 권한]
    @Override
    @Transactional
    public void createCounsel(Long studentId, CounselForm counselForm, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Member student = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Counsel counsel = Counsel.builder()
                .member(student)
                .year(counselForm.getYear())
                .semester(counselForm.getSemester())
                .content(counselForm.getContent())
                .nextCounselDate(counselForm.getNextCounselDate())
                .build();
        counselRepository.save(counsel);
    }

    // 학생 상담 수정 [선생님 권한]
    @Override
    @Transactional
    public void updateCounsel(Long counselId, CounselForm counselForm, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Counsel counsel = counselRepository.findById(counselId)
                .orElseThrow(() -> new ServiceException(ReturnCode.COUNSEL_NOT_FOUND));
        counsel.setYear(counselForm.getYear());
        counsel.setSemester(counselForm.getSemester());
        counsel.setContent(counselForm.getContent());
        counsel.setNextCounselDate(counselForm.getNextCounselDate());
    }

    // 학생 상담 삭제 [선생님 권한]
    @Override
    @Transactional
    public void deleteCounsel(Long counselId, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Counsel counsel = counselRepository.findById(counselId)
                .orElseThrow(() -> new ServiceException(ReturnCode.COUNSEL_NOT_FOUND));
        counselRepository.delete(counsel);
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = CounselPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // Counsel -> CounselDto 변환
    private CounselDto convertToCounselDto(Counsel counsel) {
        return CounselDto.builder()
                .id(counsel.getId())
                .studentId(counsel.getMember().getId())
                .year(counsel.getYear())
                .semester(counsel.getSemester())
                .content(counsel.getContent())
                .nextCounselDate(counsel.getNextCounselDate())
                .date(counsel.getCreatedAt().toLocalDate())
                .build();
    }
}
