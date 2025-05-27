package com.iEdu.domain.studentRecord.feedback.serviceImpl;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackForm;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;
import com.iEdu.domain.studentRecord.feedback.entity.Feedback;
import com.iEdu.domain.studentRecord.feedback.entity.FeedbackPage;
import com.iEdu.domain.studentRecord.feedback.repository.FeedbackRepository;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
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

import static com.iEdu.global.common.utils.Converter.convertToSemesterEnum;
import static com.iEdu.global.common.utils.RoleValidator.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;

    // 본인의 모든 피드백 조회 [학생 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackDto> getMyAllFeedback(Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선), createdAt(내림차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"), Sort.Order.desc("createdAt"))
        );
        // ROLE_STUDENT 아닌 경우 예외 처리
        validateStudentRole(loginUser);
        // visibleToStudent == true 조건 포함
        Page<Feedback> feedbackPage = feedbackRepository.findByMemberIdAndVisibleToStudentTrue(loginUser.getId(), sortedPageable);
        return feedbackPage.map(this::convertToFeedbackDto);
    }

    // 학생의 모든 피드백 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackDto> getAllFeedback(Long studentId, Pageable pageable, LoginUserDto loginUser){
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선), createdAt(내림차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"), Sort.Order.desc("createdAt"))
        );
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        validateAccessToStudent(loginUser, studentId);
        Page<Feedback> feedbackPage;
        if (loginUser.getRole() == Member.MemberRole.ROLE_TEACHER) {
            // 선생님은 모든 피드백 조회 가능
            feedbackPage = feedbackRepository.findByMemberId(studentId, sortedPageable);
        } else if (loginUser.getRole() == Member.MemberRole.ROLE_PARENT) {
            // 학부모: visibleToParent == true 조건 포함
            feedbackPage = feedbackRepository.findByMemberIdAndVisibleToParentTrue(studentId, sortedPageable);
        } else {
            // 접근 권한 없음
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        return feedbackPage.map(this::convertToFeedbackDto);
    }

    // (학년/학기)로 본인 피드백 조회 [학생 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackDto> getMyFilterFeedback(Integer year, Integer semester, Pageable pageable, LoginUserDto loginUser) {
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
        // visibleToStudent == true 조건 포함
        Page<Feedback> feedbackPage = feedbackRepository
                .findByMemberIdAndYearAndSemesterAndVisibleToStudentTrue(
                        loginUser.getId(),
                        year,
                        semesterEnum,
                        sortedPageable
                );
        return feedbackPage.map(this::convertToFeedbackDto);
    }

    // (학년/학기)로 학생 피드백 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackDto> getFilterFeedback(Long studentId, Integer year, Integer semester, Pageable pageable, LoginUserDto loginUser) {
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
        Page<Feedback> feedbackPage;
        if (loginUser.getRole() == Member.MemberRole.ROLE_TEACHER) {
            // 선생님은 모든 피드백 조회 가능
            feedbackPage = feedbackRepository.findByMemberIdAndYearAndSemester(
                    studentId, year, semesterEnum, sortedPageable
            );
        } else if (loginUser.getRole() == Member.MemberRole.ROLE_PARENT) {
            // 학부모: visibleToParent == true 조건 포함
            feedbackPage = feedbackRepository.findByMemberIdAndYearAndSemesterAndVisibleToParentTrue(
                    studentId, year, semesterEnum, sortedPageable
            );
        } else {
            // 접근 권한 없음
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        return feedbackPage.map(this::convertToFeedbackDto);
    }

    // 학생 피드백 생성 [선생님 권한]
    @Override
    @Transactional
    public void createFeedback(Long studentId, FeedbackForm feedbackForm, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Member student = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Feedback feedback = Feedback.builder()
                .member(student)
                .year(feedbackForm.getYear())
                .semester(feedbackForm.getSemester())
                .content(feedbackForm.getContent())
                .visibleToStudent(feedbackForm.getVisibleToStudent())
                .visibleToParent(feedbackForm.getVisibleToParent())
                .build();
        feedbackRepository.save(feedback);
    }

    // 학생 피드백 수정 [선생님 권한]
    @Override
    @Transactional
    public void updateFeedback(Long feedbackId, FeedbackForm feedbackForm, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ServiceException(ReturnCode.FEEDBACK_NOT_FOUND));
        feedback.setYear(feedbackForm.getYear());
        feedback.setSemester(feedbackForm.getSemester());
        feedback.setContent(feedbackForm.getContent());
        feedback.setVisibleToStudent(feedbackForm.getVisibleToStudent());
        feedback.setVisibleToParent(feedbackForm.getVisibleToParent());
    }

    // 학생 피드백 삭제 [선생님 권한]
    @Override
    @Transactional
    public void deleteFeedback(Long feedbackId, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ServiceException(ReturnCode.FEEDBACK_NOT_FOUND));
        feedbackRepository.delete(feedback);
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = FeedbackPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // Feedback -> FeedbackDto 변환
    private FeedbackDto convertToFeedbackDto(Feedback feedback) {
        return FeedbackDto.builder()
                .id(feedback.getId())
                .studentId(feedback.getMember().getId())
                .year(feedback.getYear())
                .semester(feedback.getSemester())
                .category(feedback.getCategory())
                .content(feedback.getContent())
                .date(feedback.getCreatedAt().toLocalDate())  // BaseEntity에 createdDate가 있다고 가정
                .build();
    }
}
