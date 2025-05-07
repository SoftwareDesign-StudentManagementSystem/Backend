package com.iEdu.domain.account.admin.serviceImpl;

import com.iEdu.domain.account.admin.service.AdminService;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.req.MemberForm;
import com.iEdu.domain.account.member.dto.res.DetailMemberDto;
import com.iEdu.domain.account.member.dto.res.MemberDto;
import com.iEdu.domain.account.member.dto.res.SimpleMember;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
import com.iEdu.domain.account.member.entity.MemberFollowReq;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.account.member.repository.MemberFollowRepository;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import com.iEdu.global.s3.S3Service;
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
    private final S3Service s3Service;
    private final MemberService memberService;
    private final MemberFollowRepository memberFollowRepository;

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

    // 역할별 회원 조회 [관리자 권한]
    @Override
    @Transactional
    public Page<MemberDto> getMemberByRole(String role, Pageable pageable, LoginUserDto loginUser){
        // ROLE_ADMIN이 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_ADMIN) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        // 문자열 role을 Enum으로 변환
        Member.MemberRole memberRole;
        try {
            memberRole = Member.MemberRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new ServiceException(ReturnCode.INVALID_ROLE);
        }
        Page<Member> members = memberRepository.findByRoleOrderByIdAsc(memberRole, pageable);
        return members.map(this::memberConvertToMemberDto);
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
        return memberConvertToMemberDto(member);
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
        return memberConvertToDetailMemberDto(member);
    }

    // 회원정보 수정 [관리자 권한]
    @Override
    @Transactional
    public void adminUpdateMemberInfo(MemberForm memberForm, Long memberId, LoginUserDto loginUser) {
        // ROLE_ADMIN이 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_ADMIN) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        if (memberForm.getAccountId() != null) {
            member.setAccountId(memberForm.getAccountId());
        }
        if (memberForm.getPassword() != null) {
            member.setPassword(BCrypt.hashpw(memberForm.getPassword(), BCrypt.gensalt()));
        }
        if (memberForm.getName() != null) {
            member.setName(memberForm.getName());
        }
        if (memberForm.getPhone() != null) {
            member.setPhone(memberForm.getPhone());
        }
        if (memberForm.getEmail() != null) {
            member.setEmail(memberForm.getEmail());
        }
        if (memberForm.getBirthday() != null) {
            member.setBirthday(memberForm.getBirthday());
        }
        if (memberForm.getSchoolName() != null) {
            member.setSchoolName(memberForm.getSchoolName());
        }
        if (memberForm.getYear() != null) {
            member.setYear(memberForm.getYear());
        }
        if (memberForm.getClassId() != null) {
            member.setClassId(memberForm.getClassId());
        }
        if (memberForm.getNumber() != null) {
            member.setNumber(memberForm.getNumber());
        }
        if (memberForm.getSubject() != null) {
            member.setSubject(memberForm.getSubject());
        }
        if (memberForm.getGender() != null) {
            member.setGender(memberForm.getGender());
        }
        if (memberForm.getRole() != null) {
            member.setRole(memberForm.getRole());
        }
        if (memberForm.getState() != null) {
            member.setState(memberForm.getState());
        }
        memberRepository.save(member);
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
        return members.map(this::memberConvertToMemberDto);
    }

    // 유저의 프로필 사진 삭제하기 [관리자 권한]
    @Override
    @Transactional
    public void deleteUserProfileImage(Long memberId, LoginUserDto loginUser){
        // ROLE_ADMIN이 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_ADMIN) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        if(member.getProfileImageUrl() != null){
            s3Service.deleteFile(member.getProfileImageUrl());
        }
        member.setProfileImageUrl(null);
    }

    // 학생의 팔로워 목록에서 학부모 삭제하기 [관리자 권한]
    @Override
    @Transactional
    public void removeFollowed(Long studentId, Long parentId, LoginUserDto loginUser){
        // ROLE_ADMIN이 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_ADMIN) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member followed = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Member follow = memberRepository.findById(parentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        MemberFollow memberFollow = memberFollowRepository.findByFollowAndFollowed(follow, followed)
                .orElseThrow(() -> new ServiceException(ReturnCode.FOLLOWER_NOT_FOUND));
        memberFollowRepository.delete(memberFollow);
    }

    // 회원 삭제하기 [관리자 권한]
    public void removeMember(Long memberId, LoginUserDto loginUser){
        // ROLE_ADMIN이 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_ADMIN) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        memberService.deleteMember(LoginUserDto.ConvertToLoginUserDto(member));
    }

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = MemberPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // Member를 MemberDto로 변환
    private MemberDto memberConvertToMemberDto(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .name(member.getName())
                .profileImageUrl(member.getProfileImageUrl())
                .schoolName(member.getSchoolName())
                .year(member.getYear())
                .classId(member.getClassId())
                .number(member.getNumber())
                .subject(member.getSubject())
                .role(member.getRole())
                .build();
    }

    // Member를 DetailMemberDto로 변환
    private DetailMemberDto memberConvertToDetailMemberDto(Member member) {
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
