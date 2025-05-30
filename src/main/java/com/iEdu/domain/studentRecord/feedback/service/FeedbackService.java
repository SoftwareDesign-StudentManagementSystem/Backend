package com.iEdu.domain.studentRecord.feedback.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackForm;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;
import com.iEdu.domain.studentRecord.feedback.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackService {
    // 본인의 모든 피드백 조회 [학생 권한]
    Page<FeedbackDto> getMyAllFeedback(Pageable pageable, LoginUserDto loginUser);

    // 학생의 모든 피드백 조회 [학부모/선생님 권한]
    Page<FeedbackDto> getAllFeedback(Long studentId, Pageable pageable, LoginUserDto loginUser);

    // (학년/학기)로 본인 피드백 조회 [학생 권한]
    Page<FeedbackDto> getMyFilterFeedback(Integer year, Integer semester, Pageable pageable, LoginUserDto loginUser);

    // (학년/학기)로 학생 피드백 조회 [학부모/선생님 권한]
    Page<FeedbackDto> getFilterFeedback(Long studentId, Integer year, Integer semester, Pageable pageable, LoginUserDto loginUser);

    // 학생 피드백 생성 [선생님 권한]
    void createFeedback(Long studentId, FeedbackForm feedbackForm, LoginUserDto loginUser);

    // 학생 피드백 수정 [선생님 권한]
    void updateFeedback(Long feedbackId, FeedbackForm feedbackForm, LoginUserDto loginUser);

    // 학생 피드백 삭제 [선생님 권한]
    void deleteFeedback(Long feedbackId, LoginUserDto loginUser);

    // Feedback -> FeedbackDto 변환
    FeedbackDto convertToFeedbackDto(Feedback feedback);
}
