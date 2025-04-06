package com.iEdu.domain.account.admin.controller;

import com.iEdu.domain.account.admin.service.AdminService;
import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.req.MemberForm;
import com.iEdu.domain.account.member.dto.res.DetailMemberDto;
import com.iEdu.domain.account.member.dto.res.MemberDto;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.global.common.response.IEduPage;
import com.iEdu.global.exception.ReturnCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest-api/v1/admin")
@RequiredArgsConstructor
public class ApiV1AdminController {
    private final AdminService adminService;

    // 회원가입 [관리자 권한]
    @PostMapping
    public ApiResponse<String> adminSignup(@RequestBody @Valid MemberForm memberForm, @LoginUser LoginUserDto loginUser) {
        adminService.adminSignup(memberForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 다른 멤버의 회원정보 조회 [관리자 권한]
    @GetMapping("/{memberId}")
    public ApiResponse<MemberDto> getMemberInfo(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        return ApiResponse.of(adminService.getMemberInfo(memberId, loginUser));
    }

    // 다른 멤버의 상세회원정보 조회 [관리자 권한]
    @GetMapping("/detail/{memberId}")
    public ApiResponse<DetailMemberDto> getMemberDetailInfo(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        return ApiResponse.of(adminService.getMemberDetailInfo(memberId, loginUser));
    }

    // 회원정보 수정 [관리자 권한]
    @PutMapping
    public ApiResponse<String> adminUpdateMemberInfo(@RequestBody @Valid MemberForm memberForm, @LoginUser LoginUserDto loginUser) {
        adminService.adminUpdateMemberInfo(memberForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 계정ID&이름으로 회원 검색하기 [관리자 권한]
    @GetMapping("/search")
    public ApiResponse<MemberDto> searchMemberInfo(@ModelAttribute MemberPage request, @RequestParam(value = "keyword") String keyword,
                                                   @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(adminService.searchMemberInfo(pageable, keyword, loginUser)));
    }

    // 학생의 팔로워 목록에서 학부모 삭제하기 [관리자 권한]
    @DeleteMapping("/followed/{memberId}")
    public ApiResponse<String> removeFollowed(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        adminService.removeFollowed(memberId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }
}
