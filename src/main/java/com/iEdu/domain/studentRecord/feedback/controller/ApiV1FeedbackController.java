package com.iEdu.domain.studentRecord.feedback.controller;

import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackForm;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest-api/v1/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "피드백 API")
public class ApiV1FeedbackController {

    private final FeedbackService feedbackService;

    // 피드백은 학생/학부모에 제공할 수 있는 옵션이 있어야됨

    @PostMapping
    public ResponseEntity<Map<String, String>> create(@RequestBody FeedbackForm form) {
        feedbackService.create(form);
        return ResponseEntity.ok(Map.of("message", "피드백이 추가되었습니다."));
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<List<FeedbackDto>> read(@PathVariable Long studentId) {
        return ResponseEntity.ok(feedbackService.readByStudentId(studentId));
    }
}
