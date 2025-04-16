package com.iEdu.domain.studentRecord.counsel.controller;

import com.iEdu.domain.studentRecord.counsel.dto.req.CounselRequest;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselResponse;
import com.iEdu.domain.studentRecord.counsel.service.CounselService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest-api/v1/counsel")
@RequiredArgsConstructor
@Tag(name = "Counsel API", description = "상담 기록 등록 및 조회 API")
public class ApiV1CounselController {

    private final CounselService counselService;

    @PostMapping
    @Operation(summary = "상담 기록 추가", description = "교사가 상담 기록을 추가")
    public ResponseEntity<Map<String, String>> addCounsel(@RequestBody CounselRequest request) {
        counselService.addCounsel(request);
        return ResponseEntity.ok(Collections.singletonMap("message", "상담 기록이 추가되었습니다."));
    }

    @GetMapping("/{studentId}")
    @Operation(summary = "상담 기록 조회", description = "학생 ID를 기준으로 상담 목록 조회")
    public ResponseEntity<List<CounselResponse>> getCounsels(@PathVariable Long studentId) {
        return ResponseEntity.ok(counselService.getCounselsByStudent(studentId));
    }
}
