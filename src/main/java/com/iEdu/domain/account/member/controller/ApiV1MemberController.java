package com.iEdu.domain.account.member.controller;

import com.iEdu.domain.account.auth.currentUser.CurrentUser;
import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.account.member.dto.req.*;
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
    public ApiResponse<Void> signup(@RequestBody @Valid ParentSignUpRequest parentSignUpRequest) {
        memberService.signup(parentSignUpRequest);
        return ApiResponse.success();
    }

    // 본인 회원정보 조회
    @Operation(summary = "본인 회원정보 조회")
    @GetMapping
    public ApiResponse<MemberResponse> getMyInfo(@CurrentUser CurrentUserDto currentUser) {
        return ApiResponse.success(memberService.getMyInfo(currentUser));
    }

    // 본인 상세회원정보 조회
    @Operation(summary = "본인 상세회원정보 조회")
    @GetMapping("/detail")
    public ApiResponse<DetailMemberResponse> getMyDetailInfo(@CurrentUser CurrentUserDto currentUser) {
        return ApiResponse.success(memberService.getMyDetailInfo(currentUser));
    }

    // 담당 학생들의 회원정보 조회 [선생님 권한]
    @Operation(summary = "담당 학생들의 회원정보 조회 [선생님 권한]")
    @GetMapping("/students")
    public ApiResponse<List<MemberResponse>> getMyStudentInfo(@ModelAttribute MemberPage request,
                                                              @CurrentUser CurrentUserDto currentUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(memberService.getMyStudentInfo(pageable, currentUser).getContent());
    }

    // (학년/반/번호)로 학생 조회 [선생님 권한]
    @Operation(summary = "(학년/반/번호)로 학생 조회 [선생님 권한]")
    @GetMapping("/filter")
    public ApiResponse<List<MemberResponse>> getMyFilterInfo(@ModelAttribute MemberPage request,
                                                             @RequestParam(value = "year") Integer year,
                                                             @RequestParam(value = "classId", required = false) Integer classId,
                                                             @RequestParam(value = "number", required = false) Integer number,
                                                             @CurrentUser CurrentUserDto currentUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(memberService.getMyFilterInfo(year, classId, number, pageable, currentUser));
    }

    // 학생의 회원정보 조회 [학부모/선생님 권한]
    @Operation(summary = "학생의 회원정보 조회 [학부모/선생님 권한]")
    @GetMapping("/{studentId}")
    public ApiResponse<MemberResponse> getMemberInfo(@PathVariable("studentId") Long studentId, @CurrentUser CurrentUserDto currentUser) {
        return ApiResponse.success(memberService.getMemberInfo(studentId, currentUser));
    }

    // 학생의 상세회원정보 조회 [학부모/선생님 권한]
    @Operation(summary = "학생의 상세회원정보 조회 [학부모/선생님 권한]")
    @GetMapping("/detail/{studentId}")
    public ApiResponse<DetailMemberResponse> getMemberDetailInfo(@PathVariable("studentId") Long studentId, @CurrentUser CurrentUserDto currentUser) {
        return ApiResponse.success(memberService.getMemberDetailInfo(studentId, currentUser));
    }

    // 학생/학부모 회원정보 수정 [학생/학부모 권한]
    @Operation(summary = "학생/학부모 회원정보 수정 [학생/학부모 권한]")
    @PatchMapping("/basic")
    public ApiResponse<Void> basicUpdateMemberInfo(@RequestPart(value = "basicUpdateRequest") @Valid BasicUpdateRequest basicUpdateRequest,
                                                     @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
                                                     @CurrentUser CurrentUserDto currentUser) {
        memberService.basicUpdateMemberInfo(basicUpdateRequest, imageFile, currentUser);
        return ApiResponse.success();
    }

    // 선생님 회원정보 수정 [선생님 권한]
    @Operation(summary = "선생님 회원정보 수정 [선생님 권한]")
    @PatchMapping("/teacher")
    public ApiResponse<Void> teacherUpdateMemberInfo(@RequestPart(value = "teacherUpdateRequest") @Valid TeacherUpdateRequest teacherUpdateRequest,
                                                       @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
                                                       @CurrentUser CurrentUserDto currentUser) {
        memberService.teacherUpdateMemberInfo(teacherUpdateRequest, imageFile, currentUser);
        return ApiResponse.success();
    }

    // 회원탈퇴
    @Operation(summary = "회원탈퇴")
    @DeleteMapping
    public ApiResponse<Void> deleteMember(@CurrentUser CurrentUserDto currentUser) {
        memberService.deleteMember(currentUser);
        return ApiResponse.success();
    }

    // (학번/이름)으로 학생 검색하기 [학부모/선생님 권한]
    @Operation(summary = "(학번/이름)으로 학생 검색하기 [학부모/선생님 권한]")
    @GetMapping("/search")
    public ApiResponse<List<MemberResponse>> searchMemberInfo(@ModelAttribute MemberPage request, @RequestParam(value = "keyword") String keyword,
                                                              @CurrentUser CurrentUserDto currentUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(memberService.searchMemberInfo(pageable, keyword, currentUser));
    }

    // 팔로우 요청하기 [학부모 권한]
    @Operation(summary = "팔로우 요청하기 [학부모 권한]")
    @PostMapping("/follow")
    public ApiResponse<Void> followReq(@RequestBody @Valid FollowRequest followRequest,
                                         @CurrentUser CurrentUserDto currentUser) {
        memberService.followReq(followRequest, currentUser);
        return ApiResponse.success();
    }

    // 팔로우 요청 취소하기 [학부모 권한]
    @Operation(summary = "팔로우 요청 취소하기 [학부모 권한]")
    @DeleteMapping("/follow/{memberId}")
    public ApiResponse<Void> cancelFollowReq(@PathVariable("memberId") Long memberId, @CurrentUser CurrentUserDto currentUser) {
        memberService.cancelFollowReq(memberId, currentUser);
        return ApiResponse.success();
    }

    // 팔로우 요청 수락하기 [학생 권한]
    @Operation(summary = "팔로우 요청 수락하기 [학생 권한]")
    @PostMapping("/followReq/{memberId}")
    public ApiResponse<Void> acceptFollowReq(@PathVariable("memberId") Long memberId, @CurrentUser CurrentUserDto currentUser) {
        memberService.acceptFollowReq(memberId, currentUser);
        return ApiResponse.success();
    }

    // 팔로우 요청 거절하기 [학생 권한]
    @Operation(summary = "팔로우 요청 거절하기 [학생 권한]")
    @DeleteMapping("/followReq/{memberId}")
    public ApiResponse<Void> refuseFollowReq(@PathVariable("memberId") Long memberId, @CurrentUser CurrentUserDto currentUser) {
        memberService.refuseFollowReq(memberId, currentUser);
        return ApiResponse.success();
    }

    // 팔로우 취소하기 [학부모 권한]
    @Operation(summary = "팔로우 취소하기 [학부모 권한]")
    @DeleteMapping("/followMember/{memberId}")
    public ApiResponse<Void> cancelFollow(@PathVariable("memberId") Long memberId, @CurrentUser CurrentUserDto currentUser) {
        memberService.cancelFollow(memberId, currentUser);
        return ApiResponse.success();
    }
}
