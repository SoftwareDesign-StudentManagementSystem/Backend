package com.iEdu.domain.account.member.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.req.BasicUpdateForm;
import com.iEdu.domain.account.member.dto.req.FollowForm;
import com.iEdu.domain.account.member.dto.req.ParentForm;
import com.iEdu.domain.account.member.dto.req.TeacherUpdateForm;
import com.iEdu.domain.account.member.dto.res.DetailMemberDto;
import com.iEdu.domain.account.member.dto.res.MemberDto;
import com.iEdu.domain.account.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface MemberService {

    // 학부모 회원가입
    Member signup(ParentForm parentForm);

    // 본인 회원정보 조회
    MemberDto getMyInfo(LoginUserDto loginUser);

    // 본인 상세회원정보 조회
    DetailMemberDto getMyDetailInfo(LoginUserDto loginUser);

    // 담당 학생들의 회원정보 조회 [선생님 권한]
    Page<MemberDto> getMyStudentInfo(Pageable pageable, LoginUserDto loginUser);

    // (학년/반/번호)로 학생 조회 [선생님 권한]
    Page<MemberDto> getMyFilterInfo(Integer year, Integer classId, Integer number, Pageable pageable, LoginUserDto loginUser);

    // 학생의 회원정보 조회 [학부모/선생님 권한]
    MemberDto getMemberInfo(Long studentId, LoginUserDto loginUser);

    // 학생의 상세회원정보 조회 [학부모/선생님 권한]
    DetailMemberDto getMemberDetailInfo(Long studentId, LoginUserDto loginUser);

    // 학생/학부모 회원정보 수정 [학생/학부모 권한]
    void basicUpdateMemberInfo(BasicUpdateForm basicUpdateForm, MultipartFile imageFile, LoginUserDto loginUser);

    // 선생님 회원정보 수정 [선생님 권한]
    void teacherUpdateMemberInfo(TeacherUpdateForm teacherUpdateForm, MultipartFile imageFile, LoginUserDto loginUser);

    // 회원탈퇴
    void deleteMember(LoginUserDto loginUser);

    // (학번/이름)으로 학생 검색하기 [학부모/선생님 권한]
    Page<MemberDto> searchMemberInfo(Pageable pageable, String keyword, LoginUserDto loginUser);

    // 팔로우 요청하기 [학부모 권한]
    void followReq(FollowForm followForm, LoginUserDto loginUser);

    // 팔로우 요청 취소하기 [학부모 권한]
    void cancelFollowReq(Long memberId, LoginUserDto loginUser);

    // 팔로우 요청 수락하기 [학생 권한]
    void acceptFollowReq(Long memberId, LoginUserDto loginUser);

    // 팔로우 요청 거절하기 [학생 권한]
    void refuseFollowReq(Long memberId, LoginUserDto loginUser);

    // 팔로우 취소하기 [학부모 권한]
    void cancelFollow(Long memberId, LoginUserDto loginUser);

    // 주어진 ID로 회원(Member)을 조회
    String getMemberNameById(Long memberId);

}
