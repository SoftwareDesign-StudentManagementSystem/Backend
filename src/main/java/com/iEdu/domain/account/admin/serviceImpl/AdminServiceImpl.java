package com.iEdu.domain.account.admin.serviceImpl;

import com.iEdu.domain.account.admin.service.AdminService;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.req.MemberForm;
import com.iEdu.domain.account.member.dto.res.DetailMemberDto;
import com.iEdu.domain.account.member.dto.res.MemberDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.account.member.mapper.MemberMapper;
import com.iEdu.domain.account.member.repository.MemberFollowRepository;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.global.common.utils.RoleValidator;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final MemberService memberService;
    private final MemberFollowRepository memberFollowRepository;
    private final RoleValidator roleValidator;
    private final MemberMapper memberMapper;

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

    // 회원가입 [관리자 권한]
    @Override
    @Transactional
    public Member adminSignup(MemberForm memberForm, LoginUserDto loginUser){
        // ROLE_ADMIN이 아닌 경우 예외 처리
        roleValidator.validateAdminRole(loginUser);
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
    public Page<DetailMemberDto> getMemberByRole(String role, Pageable pageable, LoginUserDto loginUser){
        checkPageSize(pageable.getPageSize());
        // ROLE_ADMIN이 아닌 경우 예외 처리
        roleValidator.validateAdminRole(loginUser);
        // 문자열 role을 Enum으로 변환
        Member.MemberRole memberRole;
        try {
            memberRole = Member.MemberRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new ServiceException(ReturnCode.INVALID_ROLE);
        }
        Page<Member> members = memberRepository.findByRoleOrderByIdAsc(memberRole, pageable);
        return members.map(memberMapper::toDetailMemberDto);
    }

    // 다른 멤버의 회원정보 조회 [관리자 권한]
    @Override
    @Transactional
    public MemberDto getMemberInfo(Long memberId, LoginUserDto loginUser) {
        // ROLE_ADMIN이 아닌 경우 예외 처리
        roleValidator.validateAdminRole(loginUser);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberMapper.toMemberDto(member);
    }

    // 다른 멤버의 상세회원정보 조회 [관리자 권한]
    @Override
    @Transactional
    public DetailMemberDto getMemberDetailInfo(Long memberId, LoginUserDto loginUser) {
        // ROLE_ADMIN이 아닌 경우 예외 처리
        roleValidator.validateAdminRole(loginUser);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberMapper.toDetailMemberDto(member);
    }

    // 회원정보 수정 [관리자 권한]
    @Override
    @Transactional
    public void adminUpdateMemberInfo(MemberForm memberForm, Long memberId, LoginUserDto loginUser) {
        // ROLE_ADMIN이 아닌 경우 예외 처리
        roleValidator.validateAdminRole(loginUser);
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
        checkPageSize(pageable.getPageSize());
        // ROLE_ADMIN이 아닌 경우 예외 처리
        roleValidator.validateAdminRole(loginUser);
        Page<Member> members = memberRepository.findByKeyword(pageable, keyword);
        return members.map(memberMapper::toMemberDto);
    }

    // 유저의 프로필 사진 삭제하기 [관리자 권한]
    @Override
    @Transactional
    public void deleteUserProfileImage(Long memberId, LoginUserDto loginUser){
        // ROLE_ADMIN이 아닌 경우 예외 처리
        roleValidator.validateAdminRole(loginUser);
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
        roleValidator.validateAdminRole(loginUser);
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
        roleValidator.validateAdminRole(loginUser);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        memberService.deleteMember(memberMapper.toLoginUserDto(member));
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = MemberPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }
}
