package com.iEdu.domain.studentRecord.counsel.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.counsel.dto.req.CounselRequest;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselResponse;
import com.iEdu.domain.studentRecord.counsel.entity.CounselPage;
import com.iEdu.domain.studentRecord.counsel.service.CounselService;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.global.common.response.IEduPage;
import com.iEdu.global.exception.ReturnCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/rest-api/v1/counsel")
@RequiredArgsConstructor
@Tag(name = "Counsel", description = "상담 API")
public class ApiV1CounselController {

    private final CounselService counselService;

    // 상담 내역은 다른 교사들과 공유될 수 있어야됨

    @PostMapping
    @Operation(summary = "상담 기록 등록", description = "교사가 학생에 대한 상담 기록을 작성합니다.")
    public ApiResponse<String> addCounsel(@RequestBody CounselRequest request,
                                          @LoginUser LoginUserDto loginUser) {
        counselService.addCounsel(request, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    @Operation(
            summary = "상담 목록 조회",
            description = """
        특정 학생의 상담 목록을 조회합니다.  
        선택적으로 날짜 범위(startDate, endDate)와 교사명(teacherName)으로 필터링할 수 있습니다.  
        기본적으로 필터가 없으면 전체 상담 내역이 조회됩니다.
        """
    )
    @GetMapping("/{studentId}")
    public ApiResponse<IEduPage<CounselResponse>> getCounsels(
            @PathVariable Long studentId,
            @ModelAttribute CounselPage request,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String teacherName,
            @LoginUser LoginUserDto loginUser) {

        // 페이지 정보
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        // 필터된 결과 요청
        Page<CounselResponse> page = counselService.getCounsels(
                studentId, pageable, loginUser, startDate, endDate, teacherName
        );

        return ApiResponse.<IEduPage<CounselResponse>>of(IEduPage.of(page));
    }
}
