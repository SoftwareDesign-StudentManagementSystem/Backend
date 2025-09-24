package com.iEdu.domain.account.admin.service;

import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.account.member.dto.req.MemberRequest;
import com.iEdu.domain.account.member.dto.res.DetailMemberResponse;
import com.iEdu.domain.account.member.dto.res.MemberResponse;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.global.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    // 회원가입 [가데이터/초기 관리자 생성]
    Member sudoSignup(MemberRequest memberRequest);

    // 회원가입 [관리자 권한]
    Member adminSignup(MemberRequest memberRequest, CurrentUserDto currentUser);

    // 역할별 회원 조회 [관리자 권한]
    PageResponse<DetailMemberResponse> getMemberByRole(String role, Pageable pageable, CurrentUserDto currentUser);

    // 다른 멤버의 회원정보 조회 [관리자 권한]
    MemberResponse getMemberInfo(Long memberId, CurrentUserDto currentUser);

    // 다른 멤버의 상세회원정보 조회 [관리자 권한]
    DetailMemberResponse getMemberDetailInfo(Long memberId, CurrentUserDto currentUser);

    // 회원정보 수정 [관리자 권한]
    void adminUpdateMemberInfo(MemberRequest memberRequest, Long memberId, CurrentUserDto currentUser);

    // 계정ID&이름으로 회원 검색하기 [관리자 권한]
    PageResponse<MemberResponse> searchMemberInfo(Pageable pageable, String keyword, CurrentUserDto currentUser);

    // 유저의 프로필 사진 삭제하기 [관리자 권한]
    void deleteUserProfileImage(Long memberId, CurrentUserDto currentUser);

    // 학생의 팔로워 목록에서 학부모 삭제하기 [관리자 권한]
    void removeFollowed(Long studentId, Long parentId, CurrentUserDto currentUser);

    // 회원 삭제하기 [관리자 권한]
    void removeMember(Long memberId, CurrentUserDto currentUser);
}
