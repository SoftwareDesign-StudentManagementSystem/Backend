package com.iEdu.domain.studentRecord.specialty.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.specialty.dto.req.SpecialtyForm;

import com.iEdu.domain.studentRecord.specialty.dto.res.SpecialtyDto;
import com.iEdu.domain.studentRecord.specialty.service.SpecialtyService;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.global.exception.ReturnCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest-api/v1/specialty")
@RequiredArgsConstructor
@Tag(name = "Specialty", description = "특기사항 API")
public class ApiV1SpecialtyController {

    private final SpecialtyService specialtyService;

    // 특기사항 전체 조회 [학부모 / 선생님 권한]
    @GetMapping("/{studentId}")
    public ApiResponse<List<SpecialtyDto>> getAllSpecialties(@PathVariable Long studentId,
                                                             @LoginUser LoginUserDto loginUser) {
        return ApiResponse.of(specialtyService.getAllSpecialties(studentId, loginUser));
    }

    // 특기사항 단건 조회
    @GetMapping("/view/{specialtyId}")
    public ApiResponse<SpecialtyDto> getSpecialty(@PathVariable Long specialtyId,
                                                  @LoginUser LoginUserDto loginUser) {
        return ApiResponse.of(specialtyService.getSpecialty(specialtyId, loginUser));
    }

    // 특기사항 등록 [선생님 전용]
    @PostMapping("/{studentId}")
    public ApiResponse<String> createSpecialty(@PathVariable Long studentId,
                                               @RequestBody @Valid SpecialtyForm form,
                                               @LoginUser LoginUserDto loginUser) {
        specialtyService.createSpecialty(studentId, form, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 특기사항 수정 [선생님 전용]
    @PutMapping("/{specialtyId}")
    public ApiResponse<String> updateSpecialty(@PathVariable Long specialtyId,
                                               @RequestBody @Valid SpecialtyForm form,
                                               @LoginUser LoginUserDto loginUser) {
        specialtyService.updateSpecialty(specialtyId, form, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 특기사항 삭제 [선생님 전용]
    @DeleteMapping("/{specialtyId}")
    public ApiResponse<String> deleteSpecialty(@PathVariable Long specialtyId,
                                               @LoginUser LoginUserDto loginUser) {
        specialtyService.deleteSpecialty(specialtyId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }
}
