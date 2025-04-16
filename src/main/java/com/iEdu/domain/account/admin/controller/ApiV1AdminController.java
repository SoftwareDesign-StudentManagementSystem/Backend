package com.iEdu.domain.account.admin.controller;

import com.iEdu.domain.account.admin.service.AdminService;
import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.req.MemberForm;
import com.iEdu.domain.account.member.dto.res.DetailMemberDto;
import com.iEdu.domain.account.member.dto.res.MemberDto;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.account.member.service.MemberService;
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
    public ApiResponse<String> adminSignup(@RequestBody @Valid MemberForm memberForm, @LoginUser LoginUserDto loginUser) {
        adminService.adminSignup(memberForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 다른 멤버의 회원정보 조회 [관리자 권한]
    @Operation(summary = "다른 멤버의 회원정보 조회 [관리자 권한]")
    @GetMapping("/{memberId}")
    public ApiResponse<MemberDto> getMemberInfo(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        return ApiResponse.of(adminService.getMemberInfo(memberId, loginUser));
    }

    // 다른 멤버의 상세회원정보 조회 [관리자 권한]
    @Operation(summary = "다른 멤버의 상세회원정보 조회 [관리자 권한]")
    @GetMapping("/detail/{memberId}")
    public ApiResponse<DetailMemberDto> getMemberDetailInfo(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        return ApiResponse.of(adminService.getMemberDetailInfo(memberId, loginUser));
    }

    // 회원정보 수정 [관리자 권한]
    @Operation(summary = "회원정보 수정 [관리자 권한]")
    @PutMapping("/{memberId}")
    public ApiResponse<String> adminUpdateMemberInfo(@PathVariable("memberId") Long memberId,
                                                     @RequestBody @Valid MemberForm memberForm,
                                                     @LoginUser LoginUserDto loginUser) {
        adminService.adminUpdateMemberInfo(memberForm, memberId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 계정ID&이름으로 회원 검색하기 [관리자 권한]
    @Operation(summary = "계정ID&이름으로 회원 검색하기 [관리자 권한]")
    @GetMapping("/search")
    public ApiResponse<MemberDto> searchMemberInfo(@ModelAttribute MemberPage request, @RequestParam(value = "keyword") String keyword,
                                                   @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(adminService.searchMemberInfo(pageable, keyword, loginUser)));
    }

    // 유저의 프로필 사진 삭제하기 [관리자 권한]
    @Operation(summary = "유저의 프로필 사진 삭제하기 [관리자 권한]")
    @DeleteMapping("/profileImage/{memberId}")
    public ApiResponse<String> deleteUserProfileImage(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        adminService.deleteUserProfileImage(memberId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 학생의 팔로워 목록에서 학부모 삭제하기 [관리자 권한]
    @Operation(summary = "학생의 팔로워 목록에서 학부모 삭제하기 [관리자 권한]")
    @DeleteMapping("/followed/{studentId}/{parentId}")
    public ApiResponse<String> removeFollowed(@PathVariable("studentId") Long studentId,
                                              @PathVariable("parentId") Long parentId,
                                              @LoginUser LoginUserDto loginUser) {
        adminService.removeFollowed(studentId, parentId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 회원 삭제하기 [관리자 권한]
    @Operation(summary = "회원 삭제하기 [관리자 권한]")
    @DeleteMapping("/{memberId}")
    public ApiResponse<String> removeMember(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        adminService.removeMember(memberId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }
}
