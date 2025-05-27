package com.iEdu.domain.studentRecord.counsel.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.counsel.dto.req.CounselForm;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselDto;
import com.iEdu.domain.studentRecord.counsel.entity.CounselPage;
import com.iEdu.domain.studentRecord.counsel.service.CounselService;

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

import java.util.List;

@RestController
@RequestMapping("/rest-api/v1/counsel")
@RequiredArgsConstructor
@Tag(name = "Counsel", description = "상담 API")
public class ApiV1CounselController {
    private final CounselService counselService;

    // 본인의 모든 상담 조회 [학생 권한]
    @Operation(summary = "본인의 모든 상담 조회 [학생 권한]")
    @GetMapping
    public ApiResponse<CounselDto> getMyAllCounsel(@ModelAttribute CounselPage request, @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(counselService.getMyAllCounsel(pageable, loginUser)));
    }

    // 학생의 모든 상담 조회 [학부모/선생님 권한]
    @Operation(summary = "학생의 모든 상담 조회 [학부모/선생님 권한]")
    @GetMapping("/{studentId}")
    public ApiResponse<CounselDto> getAllCounsel(@ModelAttribute CounselPage request,
                                                 @PathVariable("studentId") Long studentId,
                                                 @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(counselService.getAllCounsel(studentId, pageable, loginUser)));
    }

    // (학년/학기)로 본인 상담 조회 [학생 권한]
    @Operation(summary = "(학년/학기)로 본인 상담 조회 [학생 권한]")
    @GetMapping("/filter")
    public ApiResponse<CounselDto> getMyFilterCounsel(@ModelAttribute CounselPage request,
                                                      @RequestParam(value = "year") Integer year,
                                                      @RequestParam(value = "semester") Integer semester,
                                                      @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(counselService.getMyFilterCounsel(year, semester, pageable, loginUser)));
    }

    // (학년/반/번호/학기)로 학생들 상담 조회 [선생님 권한]
    @Operation(summary = "(학년/반/번호/학기)로 학생들 상담 조회 [선생님 권한]")
    @GetMapping("/filter/students")
    public ApiResponse<List<CounselDto>> getStudentsCounsel(@RequestParam(value = "year") Integer year,
                                                            @RequestParam(value = "classId") Integer classId,
                                                            @RequestParam(value = "number", required = false) Integer number,
                                                            @RequestParam(value = "semester") Integer semester,
                                                            @LoginUser LoginUserDto loginUser) {
        return ApiResponse.of(counselService.getStudentsCounsel(year, classId, number, semester, loginUser));
    }

    // (학년/학기)로 학생 상담 조회 [학부모/선생님 권한]
    @Operation(summary = "(학년/학기)로 학생 상담 조회 [학부모/선생님 권한]")
    @GetMapping("/filter/{studentId}")
    public ApiResponse<CounselDto> getFilterCounsel(@ModelAttribute CounselPage request,
                                                    @PathVariable("studentId") Long studentId,
                                                    @RequestParam(value = "year") Integer year,
                                                    @RequestParam(value = "semester") Integer semester,
                                                    @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(counselService.getFilterCounsel(studentId, year, semester, pageable, loginUser)));
    }

    // 학생 상담 생성 [선생님 권한]
    @Operation(summary = "학생 상담 생성 [선생님 권한]")
    @PostMapping("/{studentId}")
    public ApiResponse<String> createCounsel(@PathVariable("studentId") Long studentId,
                                             @RequestBody @Valid CounselForm counselForm,
                                             @LoginUser LoginUserDto loginUser) {
        counselService.createCounsel(studentId, counselForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 학생 상담 수정 [선생님 권한]
    @Operation(summary = "학생 상담 수정 [선생님 권한]")
    @PutMapping("/{counselId}")
    public ApiResponse<String> updateCounsel(@PathVariable("counselId") Long counselId,
                                             @RequestBody @Valid CounselForm counselForm,
                                             @LoginUser LoginUserDto loginUser) {
        counselService.updateCounsel(counselId, counselForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 학생 상담 삭제 [선생님 권한]
    @Operation(summary = "학생 상담 삭제 [선생님 권한]")
    @DeleteMapping("/{counselId}")
    public ApiResponse<String> deleteCounsel(@PathVariable("counselId") Long counselId,
                                             @LoginUser LoginUserDto loginUser) {
        counselService.deleteCounsel(counselId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }
}
