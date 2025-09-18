package com.iEdu.domain.account.admin.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.req.MemberRequest;
import com.iEdu.domain.account.member.dto.res.DetailMemberResponse;
import com.iEdu.domain.account.member.dto.res.MemberResponse;
import com.iEdu.domain.account.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    // 회원가입 [가데이터/초기 관리자 생성]
    Member sudoSignup(MemberRequest memberRequest);

    // 회원가입 [관리자 권한]
    Member adminSignup(MemberRequest memberRequest, LoginUserDto loginUser);

    // 역할별 회원 조회 [관리자 권한]
    Page<DetailMemberResponse> getMemberByRole(String role, Pageable pageable, LoginUserDto loginUser);

    // 다른 멤버의 회원정보 조회 [관리자 권한]
    MemberResponse getMemberInfo(Long memberId, LoginUserDto loginUser);

    // 다른 멤버의 상세회원정보 조회 [관리자 권한]
    DetailMemberResponse getMemberDetailInfo(Long memberId, LoginUserDto loginUser);

    // 회원정보 수정 [관리자 권한]
    void adminUpdateMemberInfo(MemberRequest memberRequest, Long memberId, LoginUserDto loginUser);

    // 계정ID&이름으로 회원 검색하기 [관리자 권한]
    Page<MemberResponse> searchMemberInfo(Pageable pageable, String keyword, LoginUserDto loginUser);

    // 유저의 프로필 사진 삭제하기 [관리자 권한]
    void deleteUserProfileImage(Long memberId, LoginUserDto loginUser);

    // 학생의 팔로워 목록에서 학부모 삭제하기 [관리자 권한]
    void removeFollowed(Long studentId, Long parentId, LoginUserDto loginUser);

    // 회원 삭제하기 [관리자 권한]
    void removeMember(Long memberId, LoginUserDto loginUser);
}
