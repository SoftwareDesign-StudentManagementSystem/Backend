package com.iEdu.domain.studentRecord.feedback.service;

import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackRequest;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackResponse;
import com.iEdu.domain.studentRecord.feedback.entity.Feedback;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface FeedbackService {
    // 본인의 모든 피드백 조회 [학생 권한]
    PageResponse<FeedbackResponse> getMyAllFeedback(Pageable pageable, CurrentUserDto currentUser);

    // 학생의 모든 피드백 조회 [학부모/선생님 권한]
    PageResponse<FeedbackResponse> getAllFeedback(Long studentId, Pageable pageable, CurrentUserDto currentUser);

    // (학년/학기)로 본인 피드백 조회 [학생 권한]
    PageResponse<FeedbackResponse> getMyFilterFeedback(Integer year, Semester semester, Pageable pageable, CurrentUserDto currentUser);

    // (학년/학기)로 학생 피드백 조회 [학부모/선생님 권한]
    PageResponse<FeedbackResponse> getFilterFeedback(Long studentId, Integer year, Semester semester, Pageable pageable, CurrentUserDto currentUser);

    // 학생 피드백 생성 [선생님 권한]
    void createFeedback(Long studentId, FeedbackRequest feedbackRequest, CurrentUserDto currentUser);

    // 학생 피드백 수정 [선생님 권한]
    void updateFeedback(Long feedbackId, FeedbackRequest feedbackRequest, CurrentUserDto currentUser);

    // 학생 피드백 삭제 [선생님 권한]
    void deleteFeedback(Long feedbackId, CurrentUserDto currentUser);

    // Feedback -> FeedbackDto 변환
    FeedbackResponse convertToFeedbackDto(Feedback feedback);
}
