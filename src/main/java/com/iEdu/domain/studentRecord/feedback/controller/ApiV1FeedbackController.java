package com.iEdu.domain.studentRecord.feedback.controller;

import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackForm;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;
import com.iEdu.domain.studentRecord.feedback.entity.FeedbackCategory;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest-api/v1/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "피드백 API")
public class ApiV1FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "피드백 생성", description = "학생에게 피드백을 등록합니다.")
    @PostMapping
    public ResponseEntity<Map<String, String>> create(@RequestBody FeedbackForm form) {
        feedbackService.create(form);
        return ResponseEntity.ok(Map.of("message", "피드백이 추가되었습니다."));
    }

    @Operation(summary = "학생 피드백 전체 조회", description = "특정 학생의 피드백 전체를 조회합니다.")
    @GetMapping("/{studentId}")
    public ResponseEntity<List<FeedbackDto>> read(@PathVariable Long studentId) {
        return ResponseEntity.ok(feedbackService.readByStudentId(studentId));
    }

    @Operation(summary = "학생 피드백 필터 검색", description = "학생 ID + 기간 + (선택) 교사 + (선택) 카테고리로 피드백을 검색합니다.")
    @GetMapping("/{studentId}/search")
    public ResponseEntity<List<FeedbackDto>> search(
            @Parameter(description = "학생 ID") @PathVariable Long studentId,
            @Parameter(description = "검색 시작일") @RequestParam LocalDate startDate,
            @Parameter(description = "검색 종료일") @RequestParam LocalDate endDate,
            @Parameter(description = "교사 ID (Optional)") @RequestParam(required = false) Long teacherId,
            @Parameter(description = "피드백 카테고리 (Optional)") @RequestParam(required = false) FeedbackCategory category
    ) {
        return ResponseEntity.ok(
                feedbackService.searchByFilters(studentId, teacherId, category, startDate, endDate)
        );
    }
}
