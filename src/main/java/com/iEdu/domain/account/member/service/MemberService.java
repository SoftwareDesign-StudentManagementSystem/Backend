package com.iEdu.domain.account.member.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.req.BasicUpdateRequest;
import com.iEdu.domain.account.member.dto.req.FollowRequest;
import com.iEdu.domain.account.member.dto.req.ParentSignUpRequest;
import com.iEdu.domain.account.member.dto.req.TeacherUpdateRequest;
import com.iEdu.domain.account.member.dto.res.DetailMemberResponse;
import com.iEdu.domain.account.member.dto.res.MemberResponse;
import com.iEdu.domain.account.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MemberService {
    // 학부모 회원가입
    Member signup(ParentSignUpRequest parentSignUpRequest);

    // 본인 회원정보 조회
    MemberResponse getMyInfo(LoginUserDto loginUser);

    // 본인 상세회원정보 조회
    DetailMemberResponse getMyDetailInfo(LoginUserDto loginUser);

    // 담당 학생들의 회원정보 조회 [선생님 권한]
    Page<MemberResponse> getMyStudentInfo(Pageable pageable, LoginUserDto loginUser);

    // (학년/반/번호)로 학생 조회 [선생님 권한]
    Page<MemberResponse> getMyFilterInfo(Integer year, Integer classId, Integer number, Pageable pageable, LoginUserDto loginUser);

    // 학생의 회원정보 조회 [학부모/선생님 권한]
    MemberResponse getMemberInfo(Long studentId, LoginUserDto loginUser);

    // 학생의 상세회원정보 조회 [학부모/선생님 권한]
    DetailMemberResponse getMemberDetailInfo(Long studentId, LoginUserDto loginUser);

    // 학생/학부모 회원정보 수정 [학생/학부모 권한]
    void basicUpdateMemberInfo(BasicUpdateRequest basicUpdateRequest, MultipartFile imageFile, LoginUserDto loginUser);

    // 선생님 회원정보 수정 [선생님 권한]
    void teacherUpdateMemberInfo(TeacherUpdateRequest teacherUpdateRequest, MultipartFile imageFile, LoginUserDto loginUser);

    // 회원탈퇴
    void deleteMember(LoginUserDto loginUser);

    // (학번/이름)으로 학생 검색하기 [학부모/선생님 권한]
    Page<MemberResponse> searchMemberInfo(Pageable pageable, String keyword, LoginUserDto loginUser);

    // 팔로우 요청하기 [학부모 권한]
    void followReq(FollowRequest followRequest, LoginUserDto loginUser);

    // 팔로우 요청 취소하기 [학부모 권한]
    void cancelFollowReq(Long memberId, LoginUserDto loginUser);

    // 팔로우 요청 수락하기 [학생 권한]
    void acceptFollowReq(Long memberId, LoginUserDto loginUser);

    // 팔로우 요청 거절하기 [학생 권한]
    void refuseFollowReq(Long memberId, LoginUserDto loginUser);

    // 팔로우 취소하기 [학부모 권한]
    void cancelFollow(Long memberId, LoginUserDto loginUser);

    // 학생ID로 학부모ID 조회
    List<Member> findParentsByStudentId(Long studentId);
}
