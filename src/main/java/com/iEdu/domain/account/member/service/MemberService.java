package com.iEdu.domain.account.member.service;

import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.account.member.dto.req.BasicUpdateRequest;
import com.iEdu.domain.account.member.dto.req.FollowRequest;
import com.iEdu.domain.account.member.dto.req.ParentSignUpRequest;
import com.iEdu.domain.account.member.dto.req.TeacherUpdateRequest;
import com.iEdu.domain.account.member.dto.res.DetailMemberResponse;
import com.iEdu.domain.account.member.dto.res.MemberResponse;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.global.common.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MemberService {
    // 학부모 회원가입
    Member signup(ParentSignUpRequest parentSignUpRequest);

    // 본인 회원정보 조회
    MemberResponse getMyInfo(CurrentUserDto currentUser);

    // 본인 상세회원정보 조회
    DetailMemberResponse getMyDetailInfo(CurrentUserDto currentUser);

    // 담당 학생들의 회원정보 조회 [선생님 권한]
    PageResponse<MemberResponse> getMyStudentInfo(Pageable pageable, CurrentUserDto currentUser);

    // (학년/반/번호)로 학생 조회 [선생님 권한]
    PageResponse<MemberResponse> getMyFilterInfo(Integer year, Integer classId, Integer number, Pageable pageable, CurrentUserDto currentUser);

    // 학생의 회원정보 조회 [학부모/선생님 권한]
    MemberResponse getMemberInfo(Long studentId, CurrentUserDto currentUser);

    // 학생의 상세회원정보 조회 [학부모/선생님 권한]
    DetailMemberResponse getMemberDetailInfo(Long studentId, CurrentUserDto currentUser);

    // 학생/학부모 회원정보 수정 [학생/학부모 권한]
    void basicUpdateMemberInfo(BasicUpdateRequest basicUpdateRequest, MultipartFile imageFile, CurrentUserDto currentUser);

    // 선생님 회원정보 수정 [선생님 권한]
    void teacherUpdateMemberInfo(TeacherUpdateRequest teacherUpdateRequest, MultipartFile imageFile, CurrentUserDto currentUser);

    // 회원탈퇴
    void deleteMember(CurrentUserDto currentUser);

    // (학번/이름)으로 학생 검색하기 [학부모/선생님 권한]
    PageResponse<MemberResponse> searchMemberInfo(Pageable pageable, String keyword, CurrentUserDto currentUser);

    // 팔로우 요청하기 [학부모 권한]
    void followReq(FollowRequest followRequest, CurrentUserDto currentUser);

    // 팔로우 요청 취소하기 [학부모 권한]
    void cancelFollowReq(Long memberId, CurrentUserDto currentUser);

    // 팔로우 요청 수락하기 [학생 권한]
    void acceptFollowReq(Long memberId, CurrentUserDto currentUser);

    // 팔로우 요청 거절하기 [학생 권한]
    void refuseFollowReq(Long memberId, CurrentUserDto currentUser);

    // 팔로우 취소하기 [학부모 권한]
    void cancelFollow(Long memberId, CurrentUserDto currentUser);

    // 학생ID로 학부모ID 조회
    List<Member> findParentsByStudentId(Long studentId);
}
