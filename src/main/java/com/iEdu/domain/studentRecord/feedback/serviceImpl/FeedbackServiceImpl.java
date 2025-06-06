package com.iEdu.domain.studentRecord.feedback.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackForm;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackPageCacheDto;
import com.iEdu.domain.studentRecord.feedback.entity.Feedback;
import com.iEdu.domain.studentRecord.feedback.entity.FeedbackCategory;
import com.iEdu.domain.studentRecord.feedback.entity.FeedbackPage;
import com.iEdu.domain.studentRecord.feedback.repository.FeedbackRepository;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static com.iEdu.global.common.utils.Converter.convertToSemesterEnum;
import static com.iEdu.global.common.utils.RoleValidator.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final KafkaTemplate kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

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
        validateStudentRole(loginUser);
        Semester semesterEnum = convertToSemesterEnum(semester);
        String cacheKey = String.format("feedback:%d:%d:%s:%d:%d",
                loginUser.getId(), year, semesterEnum, pageable.getPageNumber(), pageable.getPageSize());
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            JavaType type = objectMapper.getTypeFactory().constructType(FeedbackPageCacheDto.class);
            FeedbackPageCacheDto cacheDto = objectMapper.convertValue(cached, type);
            return new PageImpl<>(
                    cacheDto.getContent(),
                    PageRequest.of(cacheDto.getPageNumber(), cacheDto.getPageSize()),
                    cacheDto.getTotalElements()
            );
        }
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Feedback> feedbackPage = feedbackRepository
                .findByMemberIdAndYearAndSemesterAndVisibleToStudentTrue(
                        loginUser.getId(),
                        year,
                        semesterEnum,
                        sortedPageable
                );
        Page<FeedbackDto> feedbackDtoPage = feedbackPage.map(this::convertToFeedbackDto);
        FeedbackPageCacheDto cacheDto = FeedbackPageCacheDto.builder()
                .content(feedbackDtoPage.getContent())
                .pageNumber(feedbackDtoPage.getNumber())
                .pageSize(feedbackDtoPage.getSize())
                .totalElements(feedbackDtoPage.getTotalElements())
                .build();
        redisTemplate.opsForValue().set(cacheKey, cacheDto, Duration.ofMinutes(10));
        return feedbackDtoPage;
    }

    // (학년/학기)로 학생 피드백 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackDto> getFilterFeedback(Long studentId, Integer year, Integer semester, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        validateAccessToStudent(loginUser, studentId);
        Semester semesterEnum = convertToSemesterEnum(semester);

        String roleKey = loginUser.getRole().name(); // ROLE_TEACHER, ROLE_PARENT
        String cacheKey = String.format("feedback:%d:%d:%s:%d:%d:%s",
                studentId, year, semesterEnum, pageable.getPageNumber(), pageable.getPageSize(), roleKey);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            JavaType type = objectMapper.getTypeFactory().constructType(FeedbackPageCacheDto.class);
            FeedbackPageCacheDto cacheDto = objectMapper.convertValue(cached, type);
            return new PageImpl<>(
                    cacheDto.getContent(),
                    PageRequest.of(cacheDto.getPageNumber(), cacheDto.getPageSize()),
                    cacheDto.getTotalElements()
            );
        }
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"))
        );
        Page<Feedback> feedbackPage;
        if (loginUser.getRole() == Member.MemberRole.ROLE_TEACHER) {
            feedbackPage = feedbackRepository.findByMemberIdAndYearAndSemester(
                    studentId, year, semesterEnum, sortedPageable
            );
        } else if (loginUser.getRole() == Member.MemberRole.ROLE_PARENT) {
            feedbackPage = feedbackRepository.findByMemberIdAndYearAndSemesterAndVisibleToParentTrue(
                    studentId, year, semesterEnum, sortedPageable
            );
        } else {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Page<FeedbackDto> feedbackDtoPage = feedbackPage.map(this::convertToFeedbackDto);
        FeedbackPageCacheDto cacheDto = FeedbackPageCacheDto.builder()
                .content(feedbackDtoPage.getContent())
                .pageNumber(feedbackDtoPage.getNumber())
                .pageSize(feedbackDtoPage.getSize())
                .totalElements(feedbackDtoPage.getTotalElements())
                .build();
        redisTemplate.opsForValue().set(cacheKey, cacheDto, Duration.ofMinutes(10));
        return feedbackDtoPage;
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
                .teacherName(loginUser.getName())
                .year(feedbackForm.getYear())
                .semester(feedbackForm.getSemester())
                .date(feedbackForm.getDate())
                .category(feedbackForm.getCategory())
                .content(feedbackForm.getContent())
                .visibleToStudent(feedbackForm.getVisibleToStudent())
                .visibleToParent(feedbackForm.getVisibleToParent())
                .build();
        feedbackRepository.save(feedback);

        // 피드백 알림 생성 & Kafka 이벤트 생성
        sendFeedbackNotification(feedback, student, "새로운 피드백이 등록되었습니다.");
    }

    // 학생 피드백 수정 [선생님 권한]
    @Override
    @Transactional
    public void updateFeedback(Long feedbackId, FeedbackForm feedbackForm, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ServiceException(ReturnCode.FEEDBACK_NOT_FOUND));
        // 캐시 무효화
        evictFeedbackCache(feedback);
        feedback.setYear(feedbackForm.getYear());
        feedback.setSemester(feedbackForm.getSemester());
        feedback.setDate(feedbackForm.getDate());
        feedback.setCategory(feedbackForm.getCategory());
        feedback.setContent(feedbackForm.getContent());
        feedback.setVisibleToStudent(feedbackForm.getVisibleToStudent());
        feedback.setVisibleToParent(feedbackForm.getVisibleToParent());

        // 피드백 알림 수정 & 이벤트 생성
        sendFeedbackNotification(feedback, feedback.getMember(), "피드백이 수정되었습니다.");
    }

    // 학생 피드백 삭제 [선생님 권한]
    @Override
    @Transactional
    public void deleteFeedback(Long feedbackId, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ServiceException(ReturnCode.FEEDBACK_NOT_FOUND));
        // 캐시 무효화
        evictFeedbackCache(feedback);
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

    private void evictFeedbackCache(Feedback feedback) {
        Long studentId = feedback.getMember().getId();
        int year = feedback.getYear();
        Semester semester = feedback.getSemester();

        // 페이지 캐시를 다 삭제 (범위를 지정하지 못하므로 패턴 기반 삭제)
        String baseKeyPattern = String.format("feedback:%d:%d:%s*", studentId, year, semester);

        // Redis keys command는 scan과 함께 사용해야 안전 (keys는 성능 문제 있음)
        Set<String> keysToDelete = redisTemplate.keys(baseKeyPattern);
        if (keysToDelete != null && !keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
        }
    }

    // Feedback -> FeedbackDto 변환
    @Override
    public FeedbackDto convertToFeedbackDto(Feedback feedback) {
        return FeedbackDto.builder()
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
