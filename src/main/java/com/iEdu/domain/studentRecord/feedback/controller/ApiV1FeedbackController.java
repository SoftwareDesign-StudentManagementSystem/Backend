package com.iEdu.domain.studentRecord.feedback.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;
import com.iEdu.domain.studentRecord.feedback.entity.FeedbackPage;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.global.common.response.IEduPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest-api/v1/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "피드백 API")
public class ApiV1FeedbackController {
    private final FeedbackService feedbackService;

    // 본인/자녀의 모든 피드백 조회 [학생/학부모 권한]
    @Operation(summary = "본인/자녀의 모든 피드백 조회 [학생/학부모 권한]")
    @GetMapping
    public ApiResponse<FeedbackDto> getMyAllFeedback(@ModelAttribute FeedbackPage request, @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(feedbackService.getMyAllFeedback(pageable, loginUser)));
    }

    // 학생의 모든 피드백 조회 [선생님 권한]
    @Operation(summary = "학생의 모든 피드백 조회 [선생님 권한]")
    @GetMapping("/{studentId}")
    public ApiResponse<FeedbackDto> getAllFeedback(@ModelAttribute FeedbackPage request,
                                                   @PathVariable("studentId") Long studentId,
                                                   @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(feedbackService.getAllFeedback(studentId, pageable, loginUser)));
    }
}
