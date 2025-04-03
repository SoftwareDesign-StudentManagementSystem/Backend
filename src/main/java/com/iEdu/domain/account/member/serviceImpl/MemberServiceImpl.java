package com.iEdu.domain.account.member.serviceImpl;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.auth.service.AuthService;
import com.iEdu.domain.account.member.dto.req.MemberForm;
import com.iEdu.domain.account.member.dto.res.DetailMemberInfo;
import com.iEdu.domain.account.member.dto.res.MemberInfo;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    // 회원가입
    @Override
    @Transactional
    public Member signup(MemberForm memberForm) {
        if (memberRepository.existsByEmail((memberForm.getEmail()))) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }
        // 비밀번호가 없으면 null로 처리하거나 다른 처리를 할 수 있습니다.
        String encodedPassword = memberForm.getPassword() != null ? passwordEncoder.encode(memberForm.getPassword()) : null;
        Member member = Member.builder()
                        .name(memberForm.getName())
                        .phone(memberForm.getPhone())
                        .email(memberForm.getEmail())
                        .password(encodedPassword)  // 인코딩된 비밀번호 저장
                        .gender(memberForm.getGender())
                        .birthday(memberForm.getBirthday())
                        .build();
        memberRepository.save(member);
        return member;
    }

    // 회원정보 조회
    @Override
    @Transactional
    public MemberInfo getMyInfo(LoginUserDto loginUser) {
        return loginUserConvertToMemberInfo(loginUser);
    }

    // 본인 상세회원정보 조회
    @Override
    @Transactional
    public DetailMemberInfo getMyDetailInfo(LoginUserDto loginUser){
        return loginUserConvertToDetailMemberInfo(loginUser);
    }

    // 다른 멤버의 회원정보 조회
    @Override
    @Transactional
    public MemberInfo getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberConvertToMemberInfo(member);
    }

    // 다른 멤버의 상세회원정보 조회
    @Override
    @Transactional
    public DetailMemberInfo getMemberDetailInfo(Long memberId){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberConvertToDetailMemberInfo(member);
    }

    // 회원정보 수정
    @Override
    @Transactional
    public void updateMember(MemberForm memberForm, LoginUserDto loginUser) {
        if (memberForm.getName() != null) {
            loginUser.setName(memberForm.getName());
        }
        if (memberForm.getPhone() != null) {
            loginUser.setPhone(memberForm.getPhone());
        }
        if (memberForm.getEmail() != null) {
            loginUser.setEmail(memberForm.getEmail());
        }
        if (memberForm.getPassword() != null) {
            loginUser.setPassword(BCrypt.hashpw(memberForm.getPassword(), BCrypt.gensalt()));
        }
        if (memberForm.getGender() != null) {
            loginUser.setGender(memberForm.getGender());
        }
        if (memberForm.getBirthday() != null) {
            loginUser.setBirthday(memberForm.getBirthday());
        }
        // LoginUserDto를 Member 엔티티로 변환
        Member memberEntity = loginUser.ConvertToMember();
        memberRepository.save(memberEntity);
    }

    // 회원탈퇴
    @Override
    @Transactional
    public void deleteMember(LoginUserDto loginUser) {
        // refreshToken 삭제
        authService.logout(loginUser);
        // DB에서 회원 조회
        Member memberEntity = memberRepository.findById(loginUser.getId())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));

        // 연관된 데이터 삭제

        memberRepository.delete(memberEntity);
    }

    // 회원 검색하기
    @Override
    @Transactional
    public Page<MemberInfo> searchMemberInfo(Pageable pageable, String keyword){
        checkPageSize(pageable.getPageSize());
        Page<Member> members = memberRepository.findByKeyword(pageable, keyword);
        return members.map(this::memberConvertToMemberInfo);
    }

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = MemberPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // LoginUser를 MemberInfo로 변환
    private MemberInfo loginUserConvertToMemberInfo(LoginUserDto loginUser) {
        return MemberInfo.builder()
                .id(loginUser.getId())
                .name(loginUser.getName())
                .profileImgUrl(loginUser.getProfileImageUrl())
                .build();
    }

    // Member를 MemberInfo로 변환
    private MemberInfo memberConvertToMemberInfo(Member member) {
        return MemberInfo.builder()
                .id(member.getId())
                .name(member.getName())
                .profileImgUrl(member.getProfileImageUrl())
                .build();
    }

    // LoginUser를 DetailMemberInfo로 변환
    private DetailMemberInfo loginUserConvertToDetailMemberInfo(LoginUserDto loginUser) {
        return DetailMemberInfo.builder()
                .id(loginUser.getId())
                .name(loginUser.getName())
                .phone(loginUser.getPhone())
                .email(loginUser.getEmail())
                .gender(loginUser.getGender())
                .birthday(loginUser.getBirthday())
                .profileImgUrl(loginUser.getProfileImageUrl())
                .build();
    }

    // Member를 DetailMemberInfo로 변환
    private DetailMemberInfo memberConvertToDetailMemberInfo(Member member) {
        return DetailMemberInfo.builder()
                .id(member.getId())
                .name(member.getName())
                .phone(member.getPhone())
                .email(member.getEmail())
                .gender(member.getGender())
                .birthday(member.getBirthday())
                .profileImgUrl(member.getProfileImageUrl())
                .build();
    }
}
