package com.iEdu.domain.studentRecord.specialty.controller;

import com.iEdu.domain.account.auth.currentUser.CurrentUser;
import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.studentRecord.specialty.dto.req.SpecialtyRequest;

import com.iEdu.domain.studentRecord.specialty.dto.res.SpecialtyResponse;
import com.iEdu.domain.studentRecord.specialty.entity.SpecialtyPage;
import com.iEdu.domain.studentRecord.specialty.service.SpecialtyService;
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
@RequestMapping("/rest-api/v1/specialty")
@RequiredArgsConstructor
@Tag(name = "Specialty", description = "특기사항 API")
public class ApiV1SpecialtyController {
    private final SpecialtyService specialtyService;

    // 학생의 모든 특기사항 조회 [학부모/선생님 권한]
    @Operation(summary = "학생의 모든 특기사항 조회 [학부모/선생님 권한]")
    @GetMapping("/{studentId}")
    public ApiResponse<List<SpecialtyResponse>> getAllSpecialty(@ModelAttribute SpecialtyPage request,
                                                                @PathVariable("studentId") Long studentId,
                                                                @CurrentUser CurrentUserDto currentUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(specialtyService.getAllSpecialty(studentId, pageable, currentUser));
    }

    // (학년/학기)로 학생 특기사항 조회 [학부모/선생님 권한]
    @Operation(summary = "(학년/학기)로 학생 특기사항 조회 [학부모/선생님 권한]")
    @GetMapping("/filter/{studentId}")
    public ApiResponse<List<SpecialtyResponse>> getFilterSpecialty(@ModelAttribute SpecialtyPage request,
                                                                   @PathVariable("studentId") Long studentId,
                                                                   @RequestParam(value = "year") Integer year,
                                                                   @RequestParam(value = "semester") Semester semester,
                                                                   @CurrentUser CurrentUserDto currentUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(specialtyService.getFilterSpecialty(studentId, year, semester, pageable, currentUser));
    }

    // 학생 특기사항 생성 [선생님 권한]
    @Operation(summary = "학생 특기사항 생성 [선생님 권한]")
    @PostMapping("/{studentId}")
    public ApiResponse<Void> createSpecialty(@PathVariable("studentId") Long studentId,
                                               @RequestBody @Valid SpecialtyRequest specialtyRequest,
                                               @CurrentUser CurrentUserDto currentUser) {
        specialtyService.createSpecialty(studentId, specialtyRequest, currentUser);
        return ApiResponse.success();
    }

    // 학생 특기사항 수정 [선생님 권한]
    @Operation(summary = "학생 특기사항 수정 [선생님 권한]")
    @PatchMapping("/{specialtyId}")
    public ApiResponse<Void> updateSpecialty(@PathVariable("specialtyId") Long specialtyId,
                                               @RequestBody @Valid SpecialtyRequest specialtyRequest,
                                               @CurrentUser CurrentUserDto currentUser) {
        specialtyService.updateSpecialty(specialtyId, specialtyRequest, currentUser);
        return ApiResponse.success();
    }

    // 학생 특기사항 삭제 [선생님 권한]
    @Operation(summary = "학생 특기사항 삭제 [선생님 권한]")
    @DeleteMapping("/{specialtyId}")
    public ApiResponse<Void> deleteSpecialty(@PathVariable("specialtyId") Long specialtyId,
                                               @CurrentUser CurrentUserDto currentUser) {
        specialtyService.deleteSpecialty(specialtyId, currentUser);
        return ApiResponse.success();
    }
}
