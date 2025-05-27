package com.iEdu.domain.studentRecord.specialty.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.counsel.entity.CounselPage;
import com.iEdu.domain.studentRecord.specialty.dto.req.SpecialtyForm;

import com.iEdu.domain.studentRecord.specialty.dto.res.SpecialtyDto;
import com.iEdu.domain.studentRecord.specialty.entity.SpecialtyPage;
import com.iEdu.domain.studentRecord.specialty.service.SpecialtyService;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.global.common.response.IEduPage;
import com.iEdu.global.exception.ReturnCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.shaded.com.google.protobuf.Api;
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

    // 본인의 모든 특기사항 조회 [학생 권한]
    @Operation(summary = "본인의 모든 특기사항 조회 [학생 권한]")
    @GetMapping
    public ApiResponse<SpecialtyDto> getMyAllSpecialty(@ModelAttribute SpecialtyPage request, @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(specialtyService.getMyAllSpecialty(pageable, loginUser)));
    }

    // 학생의 모든 특기사항 조회 [학부모/선생님 권한]
    @GetMapping("/{studentId}")
    public ApiResponse<SpecialtyDto> getAllSpecialty(@ModelAttribute SpecialtyPage request,
                                                       @PathVariable("studentId") Long studentId,
                                                       @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(specialtyService.getAllSpecialty(studentId, pageable, loginUser)));
    }

    // (학년/학기)로 본인 특기사항 조회 [학생 권한]
    @Operation(summary = "(학년/학기)로 본인 특기사항 조회 [학생 권한]")
    @GetMapping("/filter")
    public ApiResponse<SpecialtyDto> getMyFilterSpecialty(@ModelAttribute SpecialtyPage request,
                                                          @RequestParam(value = "year") Integer year,
                                                          @RequestParam(value = "semester") Integer semester,
                                                          @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(specialtyService.getMyFilterSpecialty(year, semester, pageable, loginUser)));
    }

    // (학년/학기)로 학생 특기사항 조회 [학부모/선생님 권한]
    @Operation(summary = "(학년/학기)로 학생 특기사항 조회 [학부모/선생님 권한]")
    @GetMapping("/filter/{studentId}")
    public ApiResponse<SpecialtyDto> getFilterSpecialty(@ModelAttribute SpecialtyPage request,
                                                        @PathVariable("studentId") Long studentId,
                                                        @RequestParam(value = "year") Integer year,
                                                        @RequestParam(value = "semester") Integer semester,
                                                        @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(specialtyService.getFilterSpecialty(studentId, year, semester, pageable, loginUser)));
    }

    // 학생 특기사항 생성 [선생님 권한]
    @PostMapping("/{studentId}")
    public ApiResponse<String> createSpecialty(@PathVariable("studentId") Long studentId,
                                               @RequestBody @Valid SpecialtyForm specialtyForm,
                                               @LoginUser LoginUserDto loginUser) {
        specialtyService.createSpecialty(studentId, specialtyForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 학생 특기사항 수정 [선생님 권한]
    @PutMapping("/{specialtyId}")
    public ApiResponse<String> updateSpecialty(@PathVariable("specialtyId") Long specialtyId,
                                               @RequestBody @Valid SpecialtyForm specialtyForm,
                                               @LoginUser LoginUserDto loginUser) {
        specialtyService.updateSpecialty(specialtyId, specialtyForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 학생 특기사항 삭제 [선생님 권한]
    @DeleteMapping("/{specialtyId}")
    public ApiResponse<String> deleteSpecialty(@PathVariable("specialtyId") Long specialtyId,
                                               @LoginUser LoginUserDto loginUser) {
        specialtyService.deleteSpecialty(specialtyId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }
}
