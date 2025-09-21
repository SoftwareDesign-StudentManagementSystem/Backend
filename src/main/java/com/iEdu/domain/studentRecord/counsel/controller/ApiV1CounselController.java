package com.iEdu.domain.studentRecord.counsel.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.counsel.dto.req.CounselRequest;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselResponse;
import com.iEdu.domain.studentRecord.counsel.entity.CounselPage;
import com.iEdu.domain.studentRecord.counsel.service.CounselService;

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
@RequestMapping("/rest-api/v1/counsel")
@RequiredArgsConstructor
@Tag(name = "Counsel", description = "상담 API")
public class ApiV1CounselController {
    private final CounselService counselService;

    // 학생의 모든 상담 조회 [학부모/선생님 권한]
    @Operation(summary = "학생의 모든 상담 조회 [학부모/선생님 권한]")
    @GetMapping("/{studentId}")
    public ApiResponse<List<CounselResponse>> getAllCounsel(@ModelAttribute CounselPage request,
                                                            @PathVariable("studentId") Long studentId,
                                                            @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(counselService.getAllCounsel(studentId, pageable, loginUser));
    }

    // (학년/반/번호/학기)로 학생들 상담 조회 [선생님 권한]
    @Operation(summary = "(학년/반/번호/학기)로 학생들 상담 조회 [선생님 권한]")
    @GetMapping("/filter/students")
    public ApiResponse<List<CounselResponse>> getStudentsCounsel(@RequestParam(value = "year") Integer year,
                                                                 @RequestParam(value = "classId") Integer classId,
                                                                 @RequestParam(value = "number", required = false) Integer number,
                                                                 @RequestParam(value = "semester") Semester semester,
                                                                 @LoginUser LoginUserDto loginUser) {
        return ApiResponse.success(counselService.getStudentsCounsel(year, classId, number, semester, loginUser));
    }

    // (학년/학기)로 학생 상담 조회 [학부모/선생님 권한]
    @Operation(summary = "(학년/학기)로 학생 상담 조회 [학부모/선생님 권한]")
    @GetMapping("/filter/{studentId}")
    public ApiResponse<List<CounselResponse>> getFilterCounsel(@ModelAttribute CounselPage request,
                                                               @PathVariable("studentId") Long studentId,
                                                               @RequestParam(value = "year") Integer year,
                                                               @RequestParam(value = "semester") Semester semester,
                                                               @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(counselService.getFilterCounsel(studentId, year, semester, pageable, loginUser).getContent());
    }

    // 학생 상담 생성 [선생님 권한]
    @Operation(summary = "학생 상담 생성 [선생님 권한]")
    @PostMapping("/{studentId}")
    public ApiResponse<Void> createCounsel(@PathVariable("studentId") Long studentId,
                                             @RequestBody @Valid CounselRequest counselRequest,
                                             @LoginUser LoginUserDto loginUser) {
        counselService.createCounsel(studentId, counselRequest, loginUser);
        return ApiResponse.success();
    }

    // 학생 상담 수정 [선생님 권한]
    @Operation(summary = "학생 상담 수정 [선생님 권한]")
    @PatchMapping("/{counselId}")
    public ApiResponse<Void> updateCounsel(@PathVariable("counselId") Long counselId,
                                             @RequestBody @Valid CounselRequest counselRequest,
                                             @LoginUser LoginUserDto loginUser) {
        counselService.updateCounsel(counselId, counselRequest, loginUser);
        return ApiResponse.success();
    }

    // 학생 상담 삭제 [선생님 권한]
    @Operation(summary = "학생 상담 삭제 [선생님 권한]")
    @DeleteMapping("/{counselId}")
    public ApiResponse<Void> deleteCounsel(@PathVariable("counselId") Long counselId,
                                             @LoginUser LoginUserDto loginUser) {
        counselService.deleteCounsel(counselId, loginUser);
        return ApiResponse.success();
    }
}
