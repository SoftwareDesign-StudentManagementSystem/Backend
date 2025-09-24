package com.iEdu.domain.studentRecord.feedback.controller;

import com.iEdu.domain.account.auth.currentUser.CurrentUser;
import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackRequest;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackResponse;
import com.iEdu.domain.studentRecord.feedback.entity.FeedbackPage;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest-api/v1/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "피드백 API")
public class ApiV1FeedbackController {
    private final FeedbackService feedbackService;

    // 본인의 모든 피드백 조회 [학생 권한]
    @Operation(summary = "본인의 모든 피드백 조회 [학생 권한]")
    @GetMapping
    public ApiResponse<List<FeedbackResponse>> getMyAllFeedback(@ModelAttribute FeedbackPage request, @CurrentUser CurrentUserDto currentUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(feedbackService.getMyAllFeedback(pageable, currentUser));
    }

    // 학생의 모든 피드백 조회 [학부모/선생님 권한]
    @Operation(summary = "학생의 모든 피드백 조회 [학부모/선생님 권한]")
    @GetMapping("/{studentId}")
    public ApiResponse<List<FeedbackResponse>> getAllFeedback(@ModelAttribute FeedbackPage request,
                                                              @PathVariable("studentId") Long studentId,
                                                              @CurrentUser CurrentUserDto currentUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(feedbackService.getAllFeedback(studentId, pageable, currentUser));
    }

    // (학년/학기)로 본인 피드백 조회 [학생 권한]
    @Operation(summary = "(학년/학기)로 본인 피드백 조회 [학생 권한]")
    @GetMapping("/filter")
    public ApiResponse<List<FeedbackResponse>> getMyFilterFeedback(@ModelAttribute FeedbackPage request,
                                                                   @RequestParam(value = "year") Integer year,
                                                                   @RequestParam(value = "semester") Semester semester,
                                                                   @CurrentUser CurrentUserDto currentUser ){
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(feedbackService.getMyFilterFeedback(year, semester, pageable, currentUser));
    }

    // (학년/학기)로 학생 피드백 조회 [학부모/선생님 권한]
    @Operation(summary = "(학년/학기)로 학생 피드백 조회 [학부모/선생님 권한]")
    @GetMapping("/filter/{studentId}")
    public ApiResponse<List<FeedbackResponse>> getFilterFeedback(@ModelAttribute FeedbackPage request,
                                                                 @PathVariable("studentId") Long studentId,
                                                                 @RequestParam(value = "year") Integer year,
                                                                 @RequestParam(value = "semester") Semester semester,
                                                                 @CurrentUser CurrentUserDto currentUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(feedbackService.getFilterFeedback(studentId, year, semester, pageable, currentUser));
    }

    // 학생 피드백 생성 [선생님 권한]
    @Operation(summary = "학생 피드백 생성 [선생님 권한]")
    @PostMapping("/{studentId}")
    public ApiResponse<Void> createFeedback(@PathVariable("studentId") Long studentId,
                                              @RequestBody @Valid FeedbackRequest feedbackRequest,
                                              @CurrentUser CurrentUserDto currentUser) {
        feedbackService.createFeedback(studentId, feedbackRequest, currentUser);
        return ApiResponse.success();
    }

    // 학생 피드백 수정 [선생님 권한]
    @Operation(summary = "학생 피드백 수정 [선생님 권한]")
    @PatchMapping("/{feedbackId}")
    public ApiResponse<Void> updateFeedback(@PathVariable("feedbackId") Long feedbackId,
                                              @RequestBody @Valid FeedbackRequest feedbackRequest,
                                              @CurrentUser CurrentUserDto currentUser) {
        feedbackService.updateFeedback(feedbackId, feedbackRequest, currentUser);
        return ApiResponse.success();
    }

    // 학생 피드백 삭제 [선생님 권한]
    @Operation(summary = "학생 피드백 삭제 [선생님 권한]")
    @DeleteMapping("/{feedbackId}")
    public ApiResponse<Void> deleteFeedback(@PathVariable("feedbackId") Long feedbackId,
                                              @CurrentUser CurrentUserDto currentUser) {
        feedbackService.deleteFeedback(feedbackId, currentUser);
        return ApiResponse.success();
    }
}
