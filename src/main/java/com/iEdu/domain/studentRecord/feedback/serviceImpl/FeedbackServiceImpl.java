package com.iEdu.domain.studentRecord.feedback.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackRequest;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackResponse;
import com.iEdu.domain.studentRecord.feedback.entity.Feedback;
import com.iEdu.domain.studentRecord.feedback.entity.FeedbackCategory;
import com.iEdu.domain.studentRecord.feedback.entity.FeedbackPage;
import com.iEdu.domain.studentRecord.feedback.repository.FeedbackRepository;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.utils.RoleValidator;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import com.iEdu.global.redis.helper.RedisCacheEvictHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final KafkaTemplate kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RoleValidator roleValidator;
    private final RedisCacheEvictHelper redisCacheEvictHelper;

    // 본인의 모든 피드백 조회 [학생 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getMyAllFeedback(Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선), createdAt(내림차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"), Sort.Order.desc("createdAt"))
        );
        // ROLE_STUDENT 아닌 경우 예외 처리
        roleValidator.validateStudentRole(loginUser);
        // visibleToStudent == true 조건 포함
        Page<Feedback> feedbackPage = feedbackRepository.findByMemberIdAndVisibleToStudentTrue(loginUser.getId(), sortedPageable);
        return feedbackPage.map(this::convertToFeedbackDto);
    }

    // 학생의 모든 피드백 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getAllFeedback(Long studentId, Pageable pageable, LoginUserDto loginUser){
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선), createdAt(내림차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"), Sort.Order.desc("createdAt"))
        );
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateAccessToStudent(loginUser, studentId);
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
    @Cacheable(
            value = "feedback",
            key = "'student:' + #loginUser.id + ':year:' + #year + ':semester:' + #semester + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public Page<FeedbackResponse> getMyFilterFeedback(Integer year, Semester semester, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        roleValidator.validateStudentRole(loginUser);

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return feedbackRepository.findByMemberIdAndYearAndSemesterAndVisibleToStudentTrue(
                        loginUser.getId(), year, semester, sortedPageable
                )
                .map(this::convertToFeedbackDto);
    }

    // (학년/학기)로 학생 피드백 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "feedback",
            key = "'student:' + #studentId + ':year:' + #year + ':semester:' + #semester + ':role:' + #loginUser.role + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public Page<FeedbackResponse> getFilterFeedback(Long studentId, Integer year, Semester semester, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        roleValidator.validateAccessToStudent(loginUser, studentId);

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"))
        );
        Page<Feedback> feedbackPage;
        if (loginUser.getRole() == Member.MemberRole.ROLE_TEACHER) {
            feedbackPage = feedbackRepository.findByMemberIdAndYearAndSemester(
                    studentId, year, semester, sortedPageable
            );
        } else if (loginUser.getRole() == Member.MemberRole.ROLE_PARENT) {
            feedbackPage = feedbackRepository.findByMemberIdAndYearAndSemesterAndVisibleToParentTrue(
                    studentId, year, semester, sortedPageable
            );
        } else {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        return feedbackPage.map(this::convertToFeedbackDto);
    }

    // 학생 피드백 생성 [선생님 권한]
    @Override
    @Transactional
    public void createFeedback(Long studentId, FeedbackRequest feedbackRequest, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Member student = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Feedback feedback = Feedback.builder()
                .member(student)
                .teacherName(loginUser.getName())
                .year(feedbackRequest.getYear())
                .semester(feedbackRequest.getSemester())
                .date(feedbackRequest.getDate())
                .category(feedbackRequest.getCategory())
                .content(feedbackRequest.getContent())
                .visibleToStudent(feedbackRequest.getVisibleToStudent())
                .visibleToParent(feedbackRequest.getVisibleToParent())
                .build();
        feedbackRepository.save(feedback);

        // 피드백 알림 생성 & Kafka 이벤트 생성
        sendFeedbackNotification(feedback, student, "새로운 피드백이 등록되었습니다.");
    }

    // 학생 피드백 수정 [선생님 권한]
    @Override
    @Transactional
    public void updateFeedback(Long feedbackId, FeedbackRequest feedbackRequest, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ServiceException(ReturnCode.FEEDBACK_NOT_FOUND));
        feedback.setYear(feedbackRequest.getYear());
        feedback.setSemester(feedbackRequest.getSemester());
        feedback.setDate(feedbackRequest.getDate());
        feedback.setCategory(feedbackRequest.getCategory());
        feedback.setContent(feedbackRequest.getContent());
        feedback.setVisibleToStudent(feedbackRequest.getVisibleToStudent());
        feedback.setVisibleToParent(feedbackRequest.getVisibleToParent());

        // 캐시 무효화
        evictFeedbackCache(feedback.getMember().getId(), feedback.getYear(), feedback.getSemester());
        // 피드백 알림 수정 & 이벤트 생성
        sendFeedbackNotification(feedback, feedback.getMember(), "피드백이 수정되었습니다.");
    }

    // 학생 피드백 삭제 [선생님 권한]
    @Override
    @Transactional
    public void deleteFeedback(Long feedbackId, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ServiceException(ReturnCode.FEEDBACK_NOT_FOUND));
        feedbackRepository.delete(feedback);
        // 캐시 무효화
        evictFeedbackCache(feedback.getMember().getId(), feedback.getYear(), feedback.getSemester());
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = FeedbackPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // 캐시 무효화
    private void evictFeedbackCache(Long studentId, Integer year, Semester semester) {
        String prefix = "student:" + studentId + ":year:" + year + ":semester:" + semester + ":";
        redisCacheEvictHelper.evictByPrefix(prefix);
        log.debug("Feedback cache evicted for studentId={}, year={}, semester={}, prefix={}", studentId, year, semester, prefix);
    }

    // 피드백 알림 생성 & 이벤트 생성
    private void sendFeedbackNotification(Feedback feedback, Member student, String message) {
        try {
            // 학생에게 알림
            if (Boolean.TRUE.equals(feedback.getVisibleToStudent())) {
                Notification studentNotification = Notification.builder()
                        .receiverId(student.getId())
                        .objectId(feedback.getId())
                        .content(feedback.getYear() + "학년 " +
                                feedback.getSemester().toKoreanString() + " " + message)
                        .targetObject(Notification.TargetObject.Feedback)
                        .build();
                kafkaTemplate.send("feedback-topic", objectMapper.writeValueAsString(studentNotification));
            }
            // 학부모에게 알림
            if (Boolean.TRUE.equals(feedback.getVisibleToParent())) {
                List<Member> parentList = memberService.findParentsByStudentId(student.getId());
                for (Member parent : parentList) {
                    Notification parentNotification = Notification.builder()
                            .receiverId(parent.getId())
                            .objectId(feedback.getId())
                            .content("자녀의 " + feedback.getYear() + "학년 " +
                                    feedback.getSemester().toKoreanString() + " " + message)
                            .targetObject(Notification.TargetObject.Feedback)
                            .build();
                    kafkaTemplate.send("feedback-topic", objectMapper.writeValueAsString(parentNotification));
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Feedback Notification: {}", e.getMessage());
        }
    }

    // Feedback -> FeedbackDto 변환
    @Override
    public FeedbackResponse convertToFeedbackDto(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .studentId(feedback.getMember().getId())
                .teacherName(feedback.getTeacherName())
                .year(feedback.getYear())
                .semester(feedback.getSemester())
                .category(feedback.getCategory() != null ? feedback.getCategory() : FeedbackCategory.기타)
                .content(feedback.getContent())
                .date(feedback.getDate())
                .build();
    }
}
