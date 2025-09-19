package com.iEdu.domain.account.admin.controller;

import com.iEdu.domain.account.admin.service.AdminService;
import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.req.MemberRequest;
import com.iEdu.domain.account.member.dto.res.DetailMemberResponse;
import com.iEdu.domain.account.member.dto.res.MemberResponse;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.account.member.service.MemberService;
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
@RequestMapping("/rest-api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 API")
public class ApiV1AdminController {
    private final AdminService adminService;
    private final MemberService memberService;

    // 회원가입 [관리자 권한]
    @Operation(summary = "회원가입 [관리자 권한]")
    @PostMapping
    public ApiResponse<Void> adminSignup(@RequestBody @Valid MemberRequest memberRequest, @LoginUser LoginUserDto loginUser) {
        adminService.adminSignup(memberRequest, loginUser);
        return ApiResponse.success();
    }

    // 역할별 회원 조회 [관리자 권한]
    @Operation(summary = "역할별 회원 조회 [관리자 권한]")
    @GetMapping
    public ApiResponse<List<DetailMemberResponse>> getMemberByRole(@ModelAttribute MemberPage request, @RequestParam(value = "role") String role,
                                                                   @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        return ApiResponse.success(adminService.getMemberByRole(role, pageable, loginUser));
    }

    // 다른 멤버의 회원정보 조회 [관리자 권한]
    @Operation(summary = "다른 멤버의 회원정보 조회 [관리자 권한]")
    @GetMapping("/{memberId}")
    public ApiResponse<MemberResponse> getMemberInfo(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        return ApiResponse.success(adminService.getMemberInfo(memberId, loginUser));
    }

    // 다른 멤버의 상세회원정보 조회 [관리자 권한]
    @Operation(summary = "다른 멤버의 상세회원정보 조회 [관리자 권한]")
    @GetMapping("/detail/{memberId}")
    public ApiResponse<DetailMemberResponse> getMemberDetailInfo(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        return ApiResponse.success(adminService.getMemberDetailInfo(memberId, loginUser));
    }

    // 회원정보 수정 [관리자 권한]
    @Operation(summary = "회원정보 수정 [관리자 권한]")
    @PatchMapping("/{memberId}")
    public ApiResponse<Void> adminUpdateMemberInfo(@PathVariable("memberId") Long memberId,
                                                     @RequestBody @Valid MemberRequest memberRequest,
                                                     @LoginUser LoginUserDto loginUser) {
        adminService.adminUpdateMemberInfo(memberRequest, memberId, loginUser);
        return ApiResponse.success();
    }

    // 계정ID&이름으로 회원 검색하기 [관리자 권한]
    @Operation(summary = "계정ID&이름으로 회원 검색하기 [관리자 권한]")
    @GetMapping("/search")
    public ApiResponse<List<MemberResponse>> searchMemberInfo(@ModelAttribute MemberPage request, @RequestParam(value = "keyword") String keyword,
                                                              @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(adminService.searchMemberInfo(pageable, keyword, loginUser));
    }

    // 유저의 프로필 사진 삭제하기 [관리자 권한]
    @Operation(summary = "유저의 프로필 사진 삭제하기 [관리자 권한]")
    @DeleteMapping("/profileImage/{memberId}")
    public ApiResponse<Void> deleteUserProfileImage(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        adminService.deleteUserProfileImage(memberId, loginUser);
        return ApiResponse.success();
    }

    // 학생의 팔로워 목록에서 학부모 삭제하기 [관리자 권한]
    @Operation(summary = "학생의 팔로워 목록에서 학부모 삭제하기 [관리자 권한]")
    @DeleteMapping("/followed/{studentId}/{parentId}")
    public ApiResponse<Void> removeFollowed(@PathVariable("studentId") Long studentId,
                                              @PathVariable("parentId") Long parentId,
                                              @LoginUser LoginUserDto loginUser) {
        adminService.removeFollowed(studentId, parentId, loginUser);
        return ApiResponse.success();
    }

    // 회원 삭제하기 [관리자 권한]
    @Operation(summary = "회원 삭제하기 [관리자 권한]")
    @DeleteMapping("/{memberId}")
    public ApiResponse<Void> removeMember(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        adminService.removeMember(memberId, loginUser);
        return ApiResponse.success();
    }
}
