package com.iEdu.domain.account.admin.serviceImpl;

import com.iEdu.domain.account.admin.service.AdminService;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.req.MemberForm;
import com.iEdu.domain.account.member.dto.res.DetailMemberDto;
import com.iEdu.domain.account.member.dto.res.MemberDto;
import com.iEdu.domain.account.member.dto.res.SimpleMember;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입 [가데이터/초기관리자 생성]
    @Override
    @Transactional
    public Member sudoSignup(MemberForm memberForm) {
        if (memberRepository.existsByAccountId((memberForm.getAccountId()))) {
            throw new ServiceException(ReturnCode.MEMBER_ALREADY_EXISTS);
        }
        // 비밀번호가 없으면 null로 처리하거나 다른 처리를 할 수 있습니다.
        String encodedPassword = memberForm.getPassword() != null ? passwordEncoder.encode(memberForm.getPassword()) : null;
        Member member = Member.builder()
                .accountId(memberForm.getAccountId())
                .password(encodedPassword)
                .name(memberForm.getName())
                .phone(memberForm.getPhone())
                .email(memberForm.getEmail())
                .birthday(memberForm.getBirthday())
                .profileImageUrl(memberForm.getProfileImageUrl())
                .schoolName(memberForm.getSchoolName())
                .year(memberForm.getYear())
                .classId(memberForm.getClassId())
                .number(memberForm.getNumber())
                .subject(memberForm.getSubject())
                .gender(memberForm.getGender())
                .role(memberForm.getRole())
                .state(memberForm.getState())
                .build();
        memberRepository.save(member);
        return member;
    }

    // 회원가입 [관리자 권한]
    @Override
    @Transactional
    public Member adminSignup(MemberForm memberForm, LoginUserDto loginUser){
        // ROLE_ADMIN이 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_ADMIN) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        if (memberRepository.existsByAccountId((memberForm.getAccountId()))) {
            throw new ServiceException(ReturnCode.MEMBER_ALREADY_EXISTS);
        }
        // 비밀번호가 없으면 null로 처리하거나 다른 처리를 할 수 있습니다.
        String encodedPassword = memberForm.getPassword() != null ? passwordEncoder.encode(memberForm.getPassword()) : null;
        Member member = Member.builder()
                .accountId(memberForm.getAccountId())
                .password(encodedPassword)
                .name(memberForm.getName())
                .phone(memberForm.getPhone())
                .email(memberForm.getEmail())
                .birthday(memberForm.getBirthday())
                .profileImageUrl(memberForm.getProfileImageUrl())
                .schoolName(memberForm.getSchoolName())
                .year(memberForm.getYear())
                .classId(memberForm.getClassId())
                .number(memberForm.getNumber())
                .subject(memberForm.getSubject())
                .gender(memberForm.getGender())
                .role(memberForm.getRole())
                .state(memberForm.getState())
                .build();
        memberRepository.save(member);
        return member;
    }

    // 다른 멤버의 회원정보 조회 [관리자 권한]
    @Override
    @Transactional
    public MemberDto getMemberInfo(Long memberId, LoginUserDto loginUser) {
        // ROLE_ADMIN이 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_ADMIN) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberConvertToMemberInfo(member);
    }

    // 다른 멤버의 상세회원정보 조회 [관리자 권한]
    @Override
    @Transactional
    public DetailMemberDto getMemberDetailInfo(Long memberId, LoginUserDto loginUser) {
        // ROLE_ADMIN이 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_ADMIN) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberConvertToDetailMemberInfo(member);
    }

    // 회원정보 수정 [관리자 권한]
    @Override
    @Transactional
    public void adminUpdateMemberInfo(MemberForm memberForm, LoginUserDto loginUser) {
        // ROLE_ADMIN이 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_ADMIN) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        if (memberForm.getAccountId() != null) {
            loginUser.setAccountId(memberForm.getAccountId());
        }
        if (memberForm.getPassword() != null) {
            loginUser.setPassword(BCrypt.hashpw(memberForm.getPassword(), BCrypt.gensalt()));
        }
        if (memberForm.getName() != null) {
            loginUser.setName(memberForm.getName());
        }
        if (memberForm.getPhone() != null) {
            loginUser.setPhone(memberForm.getPhone());
        }
        if (memberForm.getEmail() != null) {
            loginUser.setEmail(memberForm.getEmail());
        }
        if (memberForm.getBirthday() != null) {
            loginUser.setBirthday(memberForm.getBirthday());
        }
        if (memberForm.getProfileImageUrl() != null) {
            loginUser.setProfileImageUrl(memberForm.getProfileImageUrl());
        }
        if (memberForm.getSchoolName() != null) {
            loginUser.setSchoolName(memberForm.getSchoolName());
        }
        if (memberForm.getYear() != null) {
            loginUser.setYear(memberForm.getYear());
        }
        if (memberForm.getClassId() != null) {
            loginUser.setClassId(memberForm.getClassId());
        }
        if (memberForm.getNumber() != null) {
            loginUser.setNumber(memberForm.getNumber());
        }
        if (memberForm.getSubject() != null) {
            loginUser.setSubject(memberForm.getSubject());
        }
        if (memberForm.getGender() != null) {
            loginUser.setGender(memberForm.getGender());
        }
        if (memberForm.getRole() != null) {
            loginUser.setRole(memberForm.getRole());
        }
        if (memberForm.getState() != null) {
            loginUser.setState(memberForm.getState());
        }
        // LoginUserDto를 Member 엔티티로 변환
        Member memberEntity = loginUser.ConvertToMember();
        memberRepository.save(memberEntity);
    }

    // 계정ID&이름으로 회원 검색하기 [관리자 권한]
    @Override
    @Transactional
    public Page<MemberDto> searchMemberInfo(Pageable pageable, String keyword, LoginUserDto loginUser) {
        // ROLE_ADMIN이 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_ADMIN) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        checkPageSize(pageable.getPageSize());
        Page<Member> members = memberRepository.findByKeyword(pageable, keyword);
        return members.map(this::memberConvertToMemberInfo);
    }

    // 학생의 팔로워 목록에서 학부모 삭제하기 [관리자 권한]
    @Override
    @Transactional
    public void removeFollowed(Long memberId, LoginUserDto loginUser){
        // ROLE_ADMIN이 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_ADMIN) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member follow = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Member followed = loginUser.ConvertToMember();

        // 요청받은 사용자의 followedList에서 요청자 제거
        if (followed.getFollowedList().removeIf(req -> req.getFollow().getId().equals(memberId))) {
        } else {
            throw new ServiceException(ReturnCode.FOLLOWER_NOT_FOUND);
        }
        memberRepository.save(followed);
    }

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = MemberPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // Member를 MemberInfo로 변환
    private MemberDto memberConvertToMemberInfo(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .name(member.getName())
                .profileImageUrl(member.getProfileImageUrl())
                .schoolName(member.getSchoolName())
                .year(member.getYear())
                .classId(member.getClassId())
                .number(member.getNumber())
                .role(member.getRole())
                .build();
    }

    // Member를 DetailMemberInfo로 변환
    private DetailMemberDto memberConvertToDetailMemberInfo(Member member) {
        return DetailMemberDto.builder()
                .id(member.getId())
                .accountId(member.getAccountId())
                .name(member.getName())
                .phone(member.getPhone())
                .email(member.getEmail())
                .birthday(member.getBirthday())
                .profileImageUrl(member.getProfileImageUrl())
                .schoolName(member.getSchoolName())
                .year(member.getYear())
                .classId(member.getClassId())
                .number(member.getNumber())
                .subject(member.getSubject())
                .gender(member.getGender())
                .role(member.getRole())
                // 자녀 목록 변환
                .childrenList(member.getFollowList().stream()
                        .map(MemberFollow -> SimpleMember.builder()
                                .id(MemberFollow.getFollowed().getId())
                                .name(MemberFollow.getFollowed().getName())
                                .profileImageUrl(MemberFollow.getFollowed().getProfileImageUrl())
                                .year(MemberFollow.getFollowed().getYear())
                                .classId(MemberFollow.getFollowed().getClassId())
                                .number(MemberFollow.getFollowed().getNumber())
                                .role(MemberFollow.getFollowed().getRole())
                                .build()
                        )
                        .collect(Collectors.toList())
                )
                // 부모 목록 변환
                .parentList(member.getFollowedList().stream()
                        .map(MemberFollow -> SimpleMember.builder()
                                .id(MemberFollow.getFollow().getId())
                                .name(MemberFollow.getFollow().getName())
                                .profileImageUrl(MemberFollow.getFollow().getProfileImageUrl())
                                .year(MemberFollow.getFollow().getYear())
                                .classId(MemberFollow.getFollow().getClassId())
                                .number(MemberFollow.getFollow().getNumber())
                                .role(MemberFollow.getFollow().getRole())
                                .build()
                        )
                        .collect(Collectors.toList())
                )
                // 팔로우 요청 목록 변환 (현재 사용자가 요청한 팔로우)
                .followReqList(member.getFollowReqList().stream()
                        .map(MemberFollowReq -> SimpleMember.builder()
                                .id(MemberFollowReq.getFollowRec().getId())
                                .name(MemberFollowReq.getFollowRec().getName())
                                .profileImageUrl(MemberFollowReq.getFollowRec().getProfileImageUrl())
                                .year(MemberFollowReq.getFollowRec().getYear())
                                .classId(MemberFollowReq.getFollowRec().getClassId())
                                .number(MemberFollowReq.getFollowRec().getNumber())
                                .role(MemberFollowReq.getFollowRec().getRole())
                                .build()
                        )
                        .collect(Collectors.toList())
                )
                // 팔로우 요청 받은 목록 변환 (다른 사용자가 본인한테 요청한 팔로우)
                .followRecList(member.getFollowRecList().stream()
                        .map(MemberFollowReq -> SimpleMember.builder()
                                .id(MemberFollowReq.getFollowReq().getId())
                                .name(MemberFollowReq.getFollowReq().getName())
                                .profileImageUrl(MemberFollowReq.getFollowReq().getProfileImageUrl())
                                .year(MemberFollowReq.getFollowReq().getYear())
                                .classId(MemberFollowReq.getFollowReq().getClassId())
                                .number(MemberFollowReq.getFollowReq().getNumber())
                                .role(MemberFollowReq.getFollowReq().getRole())
                                .build()
                        )
                        .collect(Collectors.toList())
                )
                .build();
    }
}
