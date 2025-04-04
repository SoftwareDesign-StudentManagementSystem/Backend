package com.iEdu.domain.account.member.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.req.MemberForm;
import com.iEdu.domain.account.member.dto.res.DetailMemberInfo;
import com.iEdu.domain.account.member.dto.res.MemberInfo;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.global.common.response.IEduPage;
import com.iEdu.global.exception.ReturnCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest-api/v1/member")
@RequiredArgsConstructor
public class ApiV1MemberController {
    private final MemberService memberService;

    // 회원가입
    @PostMapping
    public ApiResponse<String> signup(@RequestBody @Valid MemberForm memberForm) {
        memberService.signup(memberForm);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 본인 회원정보 조회
    @GetMapping
    public ApiResponse<MemberInfo> getMyInfo(@LoginUser LoginUserDto loginUser) {
        return ApiResponse.of(memberService.getMyInfo(loginUser));
    }

    // 본인 상세회원정보 조회
    @GetMapping("/detail")
    public ApiResponse<DetailMemberInfo> getMyDetailInfo(@LoginUser LoginUserDto loginUser) {
        return ApiResponse.of(memberService.getMyDetailInfo(loginUser));
    }

    // 다른 멤버의 회원정보 조회
    @GetMapping("/{memberId}")
    public ApiResponse<MemberInfo> getMemberInfo(@PathVariable("memberId") Long memberId) {
        return ApiResponse.of(memberService.getMemberInfo(memberId));
    }

    // 다른 멤버의 상세회원정보 조회
    @GetMapping("/detail/{memberId}")
    public ApiResponse<DetailMemberInfo> getMemberDetailInfo(@PathVariable("memberId") Long memberId) {
        return ApiResponse.of(memberService.getMemberDetailInfo(memberId));
    }

    // 회원정보 수정
    @PutMapping
    public ApiResponse<String> updateMemberInfo(@RequestBody @Valid MemberForm memberForm, @LoginUser LoginUserDto loginUser) {
        memberService.updateMember(memberForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 회원탈퇴
    @DeleteMapping
    public ApiResponse<String> deleteMember(@LoginUser LoginUserDto loginUser) {
        memberService.deleteMember(loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 회원 검색하기
    @GetMapping("/search")
    public ApiResponse<MemberInfo> searchMemberInfo(@ModelAttribute MemberPage request, @RequestParam(value = "keyword") String keyword) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(memberService.searchMemberInfo(pageable, keyword)));
    }

    // 팔로우 요청하기
    @PostMapping("/follow/{memberId}")
    public ApiResponse<String> followReq(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        memberService.followReq(memberId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 팔로우 요청 취소하기
    @DeleteMapping("/follow/{memberId}")
    public ApiResponse<String> cancelFollowReq(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        memberService.cancelFollowReq(memberId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 팔로우 요청 수락하기
    @PostMapping("/followReq/{memberId}")
    public ApiResponse<String> acceptFollowReq(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        memberService.acceptFollowReq(memberId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 팔로우 요청 거절하기
    @DeleteMapping("/followReq/{memberId}")
    public ApiResponse<String> refuseFollowReq(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        memberService.refuseFollowReq(memberId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 팔로우 취소하기
    @DeleteMapping("/followMember/{memberId}")
    public ApiResponse<String> cancelFollow(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        memberService.cancelFollow(memberId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 팔로워 목록에서 해당 유저 삭제하기
    @DeleteMapping("/followed/{memberId}")
    public ApiResponse<String> removeFollowed(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        memberService.removeFollowed(memberId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }
}
