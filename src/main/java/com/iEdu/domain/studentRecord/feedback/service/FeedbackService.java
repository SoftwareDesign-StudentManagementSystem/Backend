package com.iEdu.domain.studentRecord.feedback.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackService {
    // 본인/자녀의 모든 피드백 조회 [학생/학부모 권한]
    Page<FeedbackDto> getMyAllFeedback(Pageable pageable, LoginUserDto loginUser);

    // 학생의 모든 피드백 조회 [선생님 권한]
    Page<FeedbackDto> getAllFeedback(Long studentId, Pageable pageable, LoginUserDto loginUser);


}
