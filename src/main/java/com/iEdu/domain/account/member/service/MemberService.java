package com.iEdu.domain.account.member.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.req.MemberForm;
import com.iEdu.domain.account.member.dto.res.DetailMemberInfo;
import com.iEdu.domain.account.member.dto.res.MemberInfo;
import com.iEdu.domain.account.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberService {
    // 회원가입
    Member signup(MemberForm memberForm);

    // 회원정보 조회
    MemberInfo getMyInfo(LoginUserDto loginUser);

    // 본인 상세회원정보 조회
    DetailMemberInfo getMyDetailInfo(LoginUserDto loginUser);

    // 다른 멤버의 회원정보 조회
    MemberInfo getMemberInfo(Long memberId);

    // 다른 멤버의 상세회원정보 조회
    DetailMemberInfo getMemberDetailInfo(Long memberId);

    // 회원정보 수정
    void updateMember(MemberForm memberForm, LoginUserDto loginUser);

    // 회원탈퇴
    void deleteMember(LoginUserDto loginUser);

    // 회원 검색하기
    Page<MemberInfo> searchMemberInfo(Pageable pageable, String keyword);
}
