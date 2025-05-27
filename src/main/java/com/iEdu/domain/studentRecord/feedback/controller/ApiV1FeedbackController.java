package com.iEdu.domain.studentRecord.feedback.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackForm;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;
import com.iEdu.domain.studentRecord.feedback.entity.FeedbackPage;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.global.common.response.IEduPage;
import com.iEdu.global.exception.ReturnCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    // 본인의 모든 피드백 조회 [학생 권한]
    @Operation(summary = "본인의 모든 피드백 조회 [학생 권한]")
    @GetMapping
    public ApiResponse<FeedbackDto> getMyAllFeedback(@ModelAttribute FeedbackPage request, @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(feedbackService.getMyAllFeedback(pageable, loginUser)));
    }

    // 학생의 모든 피드백 조회 [학부모/선생님 권한]
    @Operation(summary = "학생의 모든 피드백 조회 [학부모/선생님 권한]")
    @GetMapping("/{studentId}")
    public ApiResponse<FeedbackDto> getAllFeedback(@ModelAttribute FeedbackPage request,
                                                   @PathVariable("studentId") Long studentId,
                                                   @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(feedbackService.getAllFeedback(studentId, pageable, loginUser)));
    }

    // (학년/학기)로 본인 피드백 조회 [학생 권한]
    @Operation(summary = "(학년/학기)로 본인 피드백 조회 [학생 권한]")
    @GetMapping("/filter")
    public ApiResponse<FeedbackDto> getMyFilterFeedback(@ModelAttribute FeedbackPage request,
                                                        @RequestParam(value = "year") Integer year,
                                                        @RequestParam(value = "semester") Integer semester,
                                                        @LoginUser LoginUserDto loginUser ){
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(feedbackService.getMyFilterFeedback(year, semester, pageable, loginUser)));
    }

    // (학년/학기)로 학생 피드백 조회 [학부모/선생님 권한]
    @Operation(summary = "(학년/학기)로 학생 피드백 조회 [학부모/선생님 권한]")
    @GetMapping("/filter/{studentId}")
    public ApiResponse<FeedbackDto> getFilterFeedback(@ModelAttribute FeedbackPage request,
                                                      @PathVariable("studentId") Long studentId,
                                                      @RequestParam(value = "year") Integer year,
                                                      @RequestParam(value = "semester") Integer semester,
                                                      @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(feedbackService.getFilterFeedback(studentId, year, semester, pageable, loginUser)));
    }

    // 학생 피드백 생성 [선생님 권한]
    @Operation(summary = "학생 피드백 생성 [선생님 권한]")
    @PostMapping("/{studentId}")
    public ApiResponse<String> createFeedback(@PathVariable("studentId") Long studentId,
                                              @RequestBody @Valid FeedbackForm feedbackForm,
                                              @LoginUser LoginUserDto loginUser) {
        feedbackService.createFeedback(studentId, feedbackForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 학생 피드백 수정 [선생님 권한]
    @Operation(summary = "학생 피드백 수정 [선생님 권한]")
    @PutMapping("/{feedbackId}")
    public ApiResponse<String> updateFeedback(@PathVariable("feedbackId") Long feedbackId,
                                              @RequestBody @Valid FeedbackForm feedbackForm,
                                              @LoginUser LoginUserDto loginUser) {
        feedbackService.updateFeedback(feedbackId, feedbackForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 학생 피드백 삭제 [선생님 권한]
    @Operation(summary = "학생 피드백 삭제 [선생님 권한]")
    @DeleteMapping("/{feedbackId}")
    public ApiResponse<String> deleteFeedback(@PathVariable("feedbackId") Long feedbackId,
                                              @LoginUser LoginUserDto loginUser) {
        feedbackService.deleteFeedback(feedbackId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }
}
