package com.iEdu.domain.account.member.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.req.*;
import com.iEdu.domain.account.member.dto.res.DetailMemberDto;
import com.iEdu.domain.account.member.dto.res.MemberDto;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.global.exception.ReturnCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/rest-api/v1/member")
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원 API")
public class ApiV1MemberController {
    private final MemberService memberService;

    // 학부모 회원가입
    @Operation(summary = "학부모 회원가입")
    @PostMapping("/parent")
    public ApiResponse<Void> signup(@RequestBody @Valid ParentForm parentForm) {
        memberService.signup(parentForm);
        return ApiResponse.success();
    }

    // 본인 회원정보 조회
    @Operation(summary = "본인 회원정보 조회")
    @GetMapping
    public ApiResponse<MemberDto> getMyInfo(@LoginUser LoginUserDto loginUser) {
        return ApiResponse.success(memberService.getMyInfo(loginUser));
    }

    // 본인 상세회원정보 조회
    @Operation(summary = "본인 상세회원정보 조회")
    @GetMapping("/detail")
    public ApiResponse<DetailMemberDto> getMyDetailInfo(@LoginUser LoginUserDto loginUser) {
        return ApiResponse.success(memberService.getMyDetailInfo(loginUser));
    }

    // 담당 학생들의 회원정보 조회 [선생님 권한]
    @Operation(summary = "담당 학생들의 회원정보 조회 [선생님 권한]")
    @GetMapping("/students")
    public ApiResponse<List<MemberDto>> getMyStudentInfo(@ModelAttribute MemberPage request,
                                                        @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(memberService.getMyStudentInfo(pageable, loginUser));
    }

    // (학년/반/번호)로 학생 조회 [선생님 권한]
    @Operation(summary = "(학년/반/번호)로 학생 조회 [선생님 권한]")
    @GetMapping("/filter")
    public ApiResponse<List<MemberDto>> getMyFilterInfo(@ModelAttribute MemberPage request,
                                                  @RequestParam(value = "year") Integer year,
                                                  @RequestParam(value = "classId", required = false) Integer classId,
                                                  @RequestParam(value = "number", required = false) Integer number,
                                                  @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(memberService.getMyFilterInfo(year, classId, number, pageable, loginUser));
    }

    // 학생의 회원정보 조회 [학부모/선생님 권한]
    @Operation(summary = "학생의 회원정보 조회 [학부모/선생님 권한]")
    @GetMapping("/{studentId}")
    public ApiResponse<MemberDto> getMemberInfo(@PathVariable("studentId") Long studentId, @LoginUser LoginUserDto loginUser) {
        return ApiResponse.success(memberService.getMemberInfo(studentId, loginUser));
    }

    // 학생의 상세회원정보 조회 [학부모/선생님 권한]
    @Operation(summary = "학생의 상세회원정보 조회 [학부모/선생님 권한]")
    @GetMapping("/detail/{studentId}")
    public ApiResponse<DetailMemberDto> getMemberDetailInfo(@PathVariable("studentId") Long studentId, @LoginUser LoginUserDto loginUser) {
        return ApiResponse.success(memberService.getMemberDetailInfo(studentId, loginUser));
    }

    // 학생/학부모 회원정보 수정 [학생/학부모 권한]
    @Operation(summary = "학생/학부모 회원정보 수정 [학생/학부모 권한]")
    @PutMapping("/basic")
    public ApiResponse<Void> basicUpdateMemberInfo(@RequestPart(value = "basicUpdateForm") @Valid BasicUpdateForm basicUpdateForm,
                                                     @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
                                                     @LoginUser LoginUserDto loginUser) {
        memberService.basicUpdateMemberInfo(basicUpdateForm, imageFile, loginUser);
        return ApiResponse.success();
    }

    // 선생님 회원정보 수정 [선생님 권한]
    @Operation(summary = "선생님 회원정보 수정 [선생님 권한]")
    @PutMapping("/teacher")
    public ApiResponse<Void> teacherUpdateMemberInfo(@RequestPart(value = "teacherUpdateForm") @Valid TeacherUpdateForm teacherUpdateForm,
                                                       @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
                                                       @LoginUser LoginUserDto loginUser) {
        memberService.teacherUpdateMemberInfo(teacherUpdateForm, imageFile, loginUser);
        return ApiResponse.success();
    }

    // 회원탈퇴
    @Operation(summary = "회원탈퇴")
    @DeleteMapping
    public ApiResponse<Void> deleteMember(@LoginUser LoginUserDto loginUser) {
        memberService.deleteMember(loginUser);
        return ApiResponse.success();
    }

    // (학번/이름)으로 학생 검색하기 [학부모/선생님 권한]
    @Operation(summary = "(학번/이름)으로 학생 검색하기 [학부모/선생님 권한]")
    @GetMapping("/search")
    public ApiResponse<List<MemberDto>> searchMemberInfo(@ModelAttribute MemberPage request, @RequestParam(value = "keyword") String keyword,
                                                   @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(memberService.searchMemberInfo(pageable, keyword, loginUser));
    }

    // 팔로우 요청하기 [학부모 권한]
    @Operation(summary = "팔로우 요청하기 [학부모 권한]")
    @PostMapping("/follow")
    public ApiResponse<Void> followReq(@RequestBody @Valid FollowForm followForm,
                                         @LoginUser LoginUserDto loginUser) {
        memberService.followReq(followForm, loginUser);
        return ApiResponse.success();
    }

    // 팔로우 요청 취소하기 [학부모 권한]
    @Operation(summary = "팔로우 요청 취소하기 [학부모 권한]")
    @DeleteMapping("/follow/{memberId}")
    public ApiResponse<Void> cancelFollowReq(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        memberService.cancelFollowReq(memberId, loginUser);
        return ApiResponse.success();
    }

    // 팔로우 요청 수락하기 [학생 권한]
    @Operation(summary = "팔로우 요청 수락하기 [학생 권한]")
    @PostMapping("/followReq/{memberId}")
    public ApiResponse<Void> acceptFollowReq(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        memberService.acceptFollowReq(memberId, loginUser);
        return ApiResponse.success();
    }

    // 팔로우 요청 거절하기 [학생 권한]
    @Operation(summary = "팔로우 요청 거절하기 [학생 권한]")
    @DeleteMapping("/followReq/{memberId}")
    public ApiResponse<Void> refuseFollowReq(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        memberService.refuseFollowReq(memberId, loginUser);
        return ApiResponse.success();
    }

    // 팔로우 취소하기 [학부모 권한]
    @Operation(summary = "팔로우 취소하기 [학부모 권한]")
    @DeleteMapping("/followMember/{memberId}")
    public ApiResponse<Void> cancelFollow(@PathVariable("memberId") Long memberId, @LoginUser LoginUserDto loginUser) {
        memberService.cancelFollow(memberId, loginUser);
        return ApiResponse.success();
    }
}
