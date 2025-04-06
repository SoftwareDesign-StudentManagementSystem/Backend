package com.iEdu.domain.account.member.serviceImpl;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.auth.service.AuthService;
import com.iEdu.domain.account.member.dto.req.BasicUpdateForm;
import com.iEdu.domain.account.member.dto.req.MemberForm;
import com.iEdu.domain.account.member.dto.req.ParentForm;
import com.iEdu.domain.account.member.dto.req.TeacherUpdateForm;
import com.iEdu.domain.account.member.dto.res.DetailMemberDto;
import com.iEdu.domain.account.member.dto.res.MemberDto;
import com.iEdu.domain.account.member.dto.res.SimpleMember;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
import com.iEdu.domain.account.member.entity.MemberFollowReq;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.account.member.repository.MemberFollowRepository;
import com.iEdu.domain.account.member.repository.MemberFollowReqRepository;
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

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final MemberFollowRepository memberFollowRepository;
    private final MemberFollowReqRepository memberFollowReqRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    // 학부모 회원가입
    @Override
    @Transactional
    public Member signup(ParentForm parentForm){
        Long accountId = parentForm.getAccountId();
        Member student = memberRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        // 1. 같은 accountId를 가진 학부모가 이미 존재하면 예외
        boolean parentExists = memberRepository.existsByAccountIdAndRole(accountId, Member.MemberRole.ROLE_PARENT);
        if (parentExists) {
            throw new ServiceException(ReturnCode.MEMBER_ALREADY_EXISTS);
        }
        // 2. 같은 accountId를 가진 학생이 존재하지 않으면 예외
        boolean studentExists = memberRepository.existsByAccountIdAndRole(accountId, Member.MemberRole.ROLE_STUDENT);
        if (!studentExists) {
            throw new ServiceException(ReturnCode.USER_NOT_FOUND);
        }
        // 3. 학생의 부모 리스트(followedList)에 이미 부모가 1명 이상이면 예외
        if (student.getFollowedList() != null && !student.getFollowedList().isEmpty()) {
            throw new ServiceException(ReturnCode.MEMBER_ALREADY_EXISTS); // 이미 부모가 등록됨
        }
        // 비밀번호가 없으면 null로 처리하거나 다른 처리를 할 수 있습니다.
        String encodedPassword = parentForm.getPassword() != null ? passwordEncoder.encode(parentForm.getPassword()) : null;
        Member member = Member.builder()
                .accountId(parentForm.getAccountId())
                .password(encodedPassword)
                .name(parentForm.getName())
                .phone(parentForm.getPhone())
                .email(parentForm.getEmail())
                .birthday(parentForm.getBirthday())
                .profileImageUrl(parentForm.getProfileImageUrl())
                .schoolName(parentForm.getSchoolName())
                .gender(parentForm.getGender())
                .build();
        memberRepository.save(member);
        return member;
    }

    // 본인 회원정보 조회
    @Override
    @Transactional
    public MemberDto getMyInfo(LoginUserDto loginUser) {
        return loginUserConvertToMemberDto(loginUser);
    }

    // 본인 상세회원정보 조회
    @Override
    @Transactional
    public DetailMemberDto getMyDetailInfo(LoginUserDto loginUser){
        return loginUserConvertToDetailMemberDto(loginUser);
    }

    // 담당 학생들의 회원정보 조회 [선생님 권한]
    @Override
    @Transactional
    public Page<MemberDto> getMyStudentInfo(Pageable pageable, LoginUserDto loginUser){
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Integer year = loginUser.getYear();
        Integer classId = loginUser.getClassId();
        Page<Member> students = memberRepository.findAllByYearAndClassIdAndRole(
                year, classId, Member.MemberRole.ROLE_STUDENT, pageable
        );
        return students.map(this::memberConvertToMemberDto);
    }

    // (학년/반/번호)로 학생 조회 [선생님 권한]
    @Override
    @Transactional
    public Page<MemberDto> getMyFilterInfo(Integer year, Integer classId, Integer number, Pageable pageable, LoginUserDto loginUser){
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Page<Member> memberPage = memberRepository.findByYearAndClassIdAndNumberAndRole(
                year, classId, number, Member.MemberRole.ROLE_STUDENT, pageable
        );
        return memberPage.map(this::memberConvertToMemberDto);
    }

    // 학생의 회원정보 조회 [학부모/선생님 권한]
    @Override
    @Transactional
    public MemberDto getMemberInfo(Long studentId, LoginUserDto loginUser) {
        // ROLE_TEACHER 또는 ROLE_PARENT이 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER &&
                loginUser.getRole() != Member.MemberRole.ROLE_PARENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member member = memberRepository.findByIdAndRole(studentId, Member.MemberRole.ROLE_STUDENT)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberConvertToMemberDto(member);
    }

    // 학생의 상세회원정보 조회 [학부모/선생님 권한]
    @Override
    @Transactional
    public DetailMemberDto getMemberDetailInfo(Long studentId, LoginUserDto loginUser) {
        Member.MemberRole role = loginUser.getRole();
        if (role == Member.MemberRole.ROLE_TEACHER) {
            // 선생님: 학생만 조회 가능
            Member student = memberRepository.findByIdAndRole(studentId, Member.MemberRole.ROLE_STUDENT)
                    .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
            return memberConvertToDetailMemberDto(student);

        } else if (role == Member.MemberRole.ROLE_PARENT) {
            // 학부모: 본인의 followList에 있는 학생(자녀)만 조회 가능
            boolean isFollowed = loginUser.getFollowList().stream()
                    .anyMatch(follow -> follow.getFollow().getId().equals(studentId));
            if (!isFollowed) {
                throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
            }
            Member student = memberRepository.findByIdAndRole(studentId, Member.MemberRole.ROLE_STUDENT)
                    .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
            return memberConvertToDetailMemberDto(student);
        }
        // 권한 없음
        throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
    }

    // 학생/학부모 회원정보 수정 [학생/학부모 권한]
    @Override
    @Transactional
    public void basicUpdateMemberInfo(BasicUpdateForm basicUpdateForm, LoginUserDto loginUser){
        // ROLE_STUDENT 또는 ROLE_PARENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT &&
                loginUser.getRole() != Member.MemberRole.ROLE_PARENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        if (basicUpdateForm.getPassword() != null) {
            loginUser.setPassword(BCrypt.hashpw(basicUpdateForm.getPassword(), BCrypt.gensalt()));
        }
        if (basicUpdateForm.getName() != null) {
            loginUser.setName(basicUpdateForm.getName());
        }
        if (basicUpdateForm.getPhone() != null) {
            loginUser.setPhone(basicUpdateForm.getPhone());
        }
        if (basicUpdateForm.getEmail() != null) {
            loginUser.setEmail(basicUpdateForm.getEmail());
        }
        if (basicUpdateForm.getBirthday() != null) {
            loginUser.setBirthday(basicUpdateForm.getBirthday());
        }
        if (basicUpdateForm.getProfileImageUrl() != null) {
            loginUser.setProfileImageUrl(basicUpdateForm.getProfileImageUrl());
        }
        if (basicUpdateForm.getSchoolName() != null) {
            loginUser.setSchoolName(basicUpdateForm.getSchoolName());
        }
        if (basicUpdateForm.getGender() != null) {
            loginUser.setGender(basicUpdateForm.getGender());
        }
        // LoginUserDto를 Member 엔티티로 변환
        Member memberEntity = loginUser.ConvertToMember();
        memberRepository.save(memberEntity);
    }

    // 선생님 회원정보 수정 [선생님 권한]
    @Override
    @Transactional
    public void teacherUpdateMemberInfo(TeacherUpdateForm teacherUpdateForm, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        if (teacherUpdateForm.getPassword() != null) {
            loginUser.setPassword(BCrypt.hashpw(teacherUpdateForm.getPassword(), BCrypt.gensalt()));
        }
        if (teacherUpdateForm.getName() != null) {
            loginUser.setName(teacherUpdateForm.getName());
        }
        if (teacherUpdateForm.getPhone() != null) {
            loginUser.setPhone(teacherUpdateForm.getPhone());
        }
        if (teacherUpdateForm.getEmail() != null) {
            loginUser.setEmail(teacherUpdateForm.getEmail());
        }
        if (teacherUpdateForm.getBirthday() != null) {
            loginUser.setBirthday(teacherUpdateForm.getBirthday());
        }
        if (teacherUpdateForm.getProfileImageUrl() != null) {
            loginUser.setProfileImageUrl(teacherUpdateForm.getProfileImageUrl());
        }
        if (teacherUpdateForm.getSchoolName() != null) {
            loginUser.setSchoolName(teacherUpdateForm.getSchoolName());
        }
        if (teacherUpdateForm.getYear() != null) {
            loginUser.setYear(teacherUpdateForm.getYear());
        }
        if (teacherUpdateForm.getClassId() != null) {
            loginUser.setClassId(teacherUpdateForm.getClassId());
        }
        if (teacherUpdateForm.getSubject() != null) {
            loginUser.setSubject(teacherUpdateForm.getSubject());
        }
        if (teacherUpdateForm.getGender() != null) {
            loginUser.setGender(teacherUpdateForm.getGender());
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

    // (학번/이름)으로 학생 검색하기 [학부모/선생님 권한]
    @Override
    @Transactional
    public Page<MemberDto> searchMemberInfo(Pageable pageable, String keyword, LoginUserDto loginUser) {
        // ROLE_TEACHER 또는 ROLE_PARENT이 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER &&
                loginUser.getRole() != Member.MemberRole.ROLE_PARENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        checkPageSize(pageable.getPageSize());
        Page<Member> members = memberRepository.findByKeywordAndRole(pageable, keyword, Member.MemberRole.ROLE_STUDENT);
        return members.map(this::memberConvertToMemberDto);
    }

    // 팔로우 요청하기 [학부모 권한]
    @Override
    @Transactional
    public void followReq(Long memberId, LoginUserDto loginUser){
        // ROLE_PARENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_PARENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member followReq = loginUser.ConvertToMember();
        Member followRec = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        // 기존 팔로우 여부 확인
        boolean already_follow = memberFollowRepository.existsByFollowAndFollowed(followReq, followRec);
        if (already_follow) {
            throw new ServiceException(ReturnCode.ALREADY_FOLLOW);
        }
        // 중복 요청 방지
        boolean already_requested = memberFollowReqRepository.existsByFollowReqAndFollowRec(followReq, followRec);
        if (already_requested) {
            throw new ServiceException(ReturnCode.ALREADY_REQUESTED);
        }
        MemberFollowReq followRequest = MemberFollowReq.builder()
                .followReq(followReq)
                .followRec(followRec)
                .build();
        memberFollowReqRepository.save(followRequest);
    }

    // 팔로우 요청 취소하기 [학부모 권한]
    @Override
    @Transactional
    public void cancelFollowReq(Long memberId, LoginUserDto loginUser){
        // ROLE_PARENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_PARENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member followReq = loginUser.ConvertToMember();
        Member followRec = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        MemberFollowReq followRequest = memberFollowReqRepository.findByFollowReqAndFollowRec(followReq, followRec)
                .orElseThrow(() -> new ServiceException(ReturnCode.REQUEST_NOT_FOUND));
        memberFollowReqRepository.delete(followRequest);
    }

    // 팔로우 요청 수락하기 [학생 권한]
    @Override
    @Transactional
    public void acceptFollowReq(Long memberId, LoginUserDto loginUser){
        // ROLE_STUDENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Member receiver = loginUser.ConvertToMember();

        // 요청받은 사용자의 followRecList에서 요청자 제거 및 followedList에 추가
        if (receiver.getFollowRecList().removeIf(req -> req.getFollowReq().getId().equals(memberId))) {
            receiver.getFollowList().add(new MemberFollow(requester, receiver));
        } else {
            throw new ServiceException(ReturnCode.REQUEST_NOT_FOUND);
        }
        memberRepository.save(receiver);
    }

    // 팔로우 요청 거절하기 [학생 권한]
    @Override
    @Transactional
    public void refuseFollowReq(Long memberId, LoginUserDto loginUser){
        // ROLE_STUDENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Member receiver = loginUser.ConvertToMember();

        // 요청받은 사용자의 followRecList에서 요청자 제거
        if (receiver.getFollowRecList().removeIf(req -> req.getFollowReq().getId().equals(memberId))) {
        } else {
            throw new ServiceException(ReturnCode.REQUEST_NOT_FOUND);
        }
        memberRepository.save(receiver);
    }

    // 팔로우 취소하기 [학부모 권한]
    @Override
    @Transactional
    public void cancelFollow(Long memberId, LoginUserDto loginUser){
        // ROLE_PARENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_PARENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member follow = loginUser.ConvertToMember();
        Member followed = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        MemberFollow memberFollow = memberFollowRepository.findByFollowAndFollowed(follow, followed)
                .orElseThrow(() -> new ServiceException(ReturnCode.FOLLOWER_NOT_FOUND));
        memberFollowRepository.delete(memberFollow);
    }

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = MemberPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // LoginUser를 MemberInfo로 변환
    private MemberDto loginUserConvertToMemberDto(LoginUserDto loginUser) {
        return MemberDto.builder()
                .id(loginUser.getId())
                .name(loginUser.getName())
                .profileImageUrl(loginUser.getProfileImageUrl())
                .schoolName(loginUser.getSchoolName())
                .year(loginUser.getYear())
                .classId(loginUser.getClassId())
                .number(loginUser.getNumber())
                .role(loginUser.getRole())
                .build();
    }

    // Member를 MemberInfo로 변환
    private MemberDto memberConvertToMemberDto(Member member) {
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

    // LoginUser를 DetailMemberInfo로 변환
    private DetailMemberDto loginUserConvertToDetailMemberDto(LoginUserDto loginUser) {
        return DetailMemberDto.builder()
                .id(loginUser.getId())
                .accountId(loginUser.getAccountId())
                .name(loginUser.getName())
                .phone(loginUser.getPhone())
                .email(loginUser.getEmail())
                .birthday(loginUser.getBirthday())
                .profileImageUrl(loginUser.getProfileImageUrl())
                .schoolName(loginUser.getSchoolName())
                .year(loginUser.getYear())
                .classId(loginUser.getClassId())
                .number(loginUser.getNumber())
                .subject(loginUser.getSubject())
                .gender(loginUser.getGender())
                .role(loginUser.getRole())
                // 자녀 목록 변환
                .childrenList(loginUser.getFollowList().stream()
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
                .parentList(loginUser.getFollowedList().stream()
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
                .followReqList(loginUser.getFollowReqList().stream()
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
                .followRecList(loginUser.getFollowRecList().stream()
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

    // Member를 DetailMemberInfo로 변환
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
