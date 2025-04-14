package com.iEdu.domain.studentRecord.feedback.controller;

import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackForm;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest-api/v1/feedback")
@RequiredArgsConstructor
public class ApiV1FeedbackController {

    private final FeedbackService feedbackService;

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
