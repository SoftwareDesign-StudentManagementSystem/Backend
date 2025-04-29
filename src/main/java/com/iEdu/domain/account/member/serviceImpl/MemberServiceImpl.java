package com.iEdu.domain.account.member.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.auth.service.AuthService;
import com.iEdu.domain.account.member.dto.req.BasicUpdateForm;
import com.iEdu.domain.account.member.dto.req.FollowForm;
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
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import com.iEdu.global.s3.S3Service;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private final S3Service s3Service;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 학부모 회원가입
    @Override
    @Transactional
    public Member signup(ParentForm parentForm){
        Long accountId = parentForm.getAccountId();
        String email = parentForm.getEmail();

        // 0. accountId 길이 검증 (11자리인지 확인)
        if (String.valueOf(accountId).length() != 11) {
            throw new ServiceException(ReturnCode.INVALID_ACCOUNT_ID);
        }
        // 1. 같은 accountId를 가진 학부모가 이미 존재하면 예외
        boolean parentAccountIdExists = memberRepository.existsByAccountIdAndRole(accountId, Member.MemberRole.ROLE_PARENT);
        if (parentAccountIdExists) {
            throw new ServiceException(ReturnCode.MEMBER_ALREADY_EXISTS);
        }
        // 2. 같은 email을 가진 학부모가 이미 존재하면 예외
        boolean parentEmailExists = memberRepository.existsByEmailAndRole(email, Member.MemberRole.ROLE_PARENT);
        if (parentEmailExists) {
            throw new ServiceException(ReturnCode.MEMBER_ALREADY_EXISTS);
        }
        // 3. 같은 accountId를 가진 학생이 존재하지 않으면 예외
        boolean studentExists = memberRepository.existsByAccountIdAndRole(accountId / 100, Member.MemberRole.ROLE_STUDENT);
        if (!studentExists) {
            throw new ServiceException(ReturnCode.USER_NOT_FOUND);
        }
        Member student = memberRepository.findByAccountId(accountId / 100)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        // 4. 학생의 부모 리스트(followedList)에 이미 부모가 1명 이상이면 예외
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
        if (classId == null) {
            throw new ServiceException(ReturnCode.CLASSID_NOT_FOUND);
        }
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
        Member.MemberRole role = loginUser.getRole();
        if (role == Member.MemberRole.ROLE_TEACHER) {
            // 선생님: 학생만 조회 가능
            Member student = memberRepository.findByIdAndRole(studentId, Member.MemberRole.ROLE_STUDENT)
                    .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
            return memberConvertToMemberDto(student);

        } else if (role == Member.MemberRole.ROLE_PARENT) {
            // 학부모: 본인의 followList에 있는 학생(자녀)만 조회 가능
            boolean isFollowed = loginUser.getFollowList().stream()
                    .anyMatch(follow -> follow.getFollow().getId().equals(studentId));
            if (!isFollowed) {
                throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
            }
            Member student = memberRepository.findByIdAndRole(studentId, Member.MemberRole.ROLE_STUDENT)
                    .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
            return memberConvertToMemberDto(student);
        }
        // 권한 없음
        throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
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
    public void basicUpdateMemberInfo(BasicUpdateForm basicUpdateForm, MultipartFile imageFile, LoginUserDto loginUser){
        // ROLE_STUDENT 또는 ROLE_PARENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT &&
                loginUser.getRole() != Member.MemberRole.ROLE_PARENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        // 기존 이미지 삭제 후 입력 받은 이미지 S3에 저장
        String imageUrl = loginUser.getProfileImageUrl(); // 기본적으로 기존 이미지 URL을 사용
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 없으면 바로 새로운 이미지 저장
            if (imageUrl != null && !imageUrl.isEmpty()) {
                s3Service.deleteFile(imageUrl);
            }
            try {
                imageUrl = s3Service.uploadFile(imageFile, "profile-image");
            } catch (IOException e) {
                throw new ServiceException(ReturnCode.INTERNAL_ERROR);
            }
        } else {
            // imageFile이 없으면 기존 이미지가 있다면 삭제한다
            if (imageUrl != null && !imageUrl.isEmpty()) {
                s3Service.deleteFile(imageUrl); // 기존 이미지 삭제
            }
            imageUrl = null;
        }
        if (basicUpdateForm.getPassword() != null) {
            loginUser.setPassword(BCrypt.hashpw(basicUpdateForm.getPassword(), BCrypt.gensalt()));
        }
        if (basicUpdateForm.getName() != null) {
            loginUser.setName(basicUpdateForm.getName());
        }
        if (basicUpdateForm.getBirthday() != null) {
            loginUser.setBirthday(basicUpdateForm.getBirthday());
        }
        if (basicUpdateForm.getSchoolName() != null) {
            loginUser.setSchoolName(basicUpdateForm.getSchoolName());
        }
        if (basicUpdateForm.getGender() != null) {
            loginUser.setGender(basicUpdateForm.getGender());
        }
        loginUser.setPhone(basicUpdateForm.getPhone());
        loginUser.setEmail(basicUpdateForm.getEmail());
        loginUser.setProfileImageUrl(imageUrl);
        // LoginUserDto를 Member 엔티티로 변환
        Member memberEntity = loginUser.ConvertToMember();
        memberRepository.save(memberEntity);
    }

    // 선생님 회원정보 수정 [선생님 권한]
    @Override
    @Transactional
    public void teacherUpdateMemberInfo(TeacherUpdateForm teacherUpdateForm, MultipartFile imageFile, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        // 기존 이미지 삭제 후 입력 받은 이미지 S3에 저장
        String imageUrl = loginUser.getProfileImageUrl(); // 기본적으로 기존 이미지 URL을 사용
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 없으면 바로 새로운 이미지 저장
            if (imageUrl != null && !imageUrl.isEmpty()) {
                s3Service.deleteFile(imageUrl);
            }
            try {
                imageUrl = s3Service.uploadFile(imageFile, "profile-image");
            } catch (IOException e) {
                throw new ServiceException(ReturnCode.INTERNAL_ERROR);
            }
        } else {
            // imageFile이 없으면 기존 이미지가 있다면 삭제한다
            if (imageUrl != null && !imageUrl.isEmpty()) {
                s3Service.deleteFile(imageUrl); // 기존 이미지 삭제
            }
            imageUrl = null;
        }
        if (teacherUpdateForm.getPassword() != null) {
            loginUser.setPassword(BCrypt.hashpw(teacherUpdateForm.getPassword(), BCrypt.gensalt()));
        }
        if (teacherUpdateForm.getName() != null) {
            loginUser.setName(teacherUpdateForm.getName());
        }
        if (teacherUpdateForm.getBirthday() != null) {
            loginUser.setBirthday(teacherUpdateForm.getBirthday());
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
        loginUser.setPhone(teacherUpdateForm.getPhone());
        loginUser.setEmail(teacherUpdateForm.getEmail());
        loginUser.setProfileImageUrl(imageUrl);
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
    public void followReq(FollowForm followForm, LoginUserDto loginUser){
        // ROLE_PARENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_PARENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member followReq = loginUser.ConvertToMember();
        Member followRec = memberRepository.findByNameAndYearAndClassIdAndNumberAndBirthday(
                followForm.getName(),
                followForm.getYear(),
                followForm.getClassId(),
                followForm.getNumber(),
                followForm.getBirthday()
        ).orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
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
        MemberFollowReq memberFollowReq = MemberFollowReq.builder()
                .followReq(followReq)
                .followRec(followRec)
                .build();
        memberFollowReqRepository.save(memberFollowReq);
        // 팔로우 요청 이벤트 생성
        Notification notification = Notification.builder()
                .receiverId(followRec.getId())
                .objectId(memberFollowReq.getId())
                .content(loginUser.getName() + " 학부모님이 팔로우를 요청하였습니다")
                .targetObject(Notification.TargetObject.Member)
                .build();
        try {
            String message = objectMapper.writeValueAsString(notification);
            kafkaTemplate.send("follow-topic", message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Notification: {}", e.getMessage());
        }
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
        MemberFollowReq memberFollowReq = memberFollowReqRepository.findByFollowReqAndFollowRec(followReq, followRec)
                .orElseThrow(() -> new ServiceException(ReturnCode.REQUEST_NOT_FOUND));
        memberFollowReqRepository.delete(memberFollowReq);
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
        MemberFollowReq followReq = memberFollowReqRepository.findByFollowReqAndFollowRec(requester, receiver)
                .orElseThrow(() -> new ServiceException(ReturnCode.REQUEST_NOT_FOUND));
        memberFollowReqRepository.delete(followReq);
        MemberFollow memberFollow = MemberFollow.builder()
                .follow(requester)
                .followed(receiver)
                .build();
        memberFollowRepository.save(memberFollow);
        // 팔로우 수락 이벤트 생성
        Notification notification = Notification.builder()
                .receiverId(memberId)
                .objectId(memberFollow.getId())
                .content(loginUser.getName() + " 학생이 팔로우 요청을 수락하였습니다")
                .targetObject(Notification.TargetObject.Member)
                .build();
        try {
            String message = objectMapper.writeValueAsString(notification);
            kafkaTemplate.send("follow-topic", message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Notification: {}", e.getMessage());
        }
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
        MemberFollowReq memberFollowReq = memberFollowReqRepository.findByFollowReqAndFollowRec(requester, receiver)
                .orElseThrow(() -> new ServiceException(ReturnCode.REQUEST_NOT_FOUND));
        memberFollowReqRepository.delete(memberFollowReq);
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
                .orElseThrow(() -> new ServiceException(ReturnCode.FOLLOW_NOT_FOUND));
        memberFollowRepository.delete(memberFollow);
    }

    // 회원ID로 회원이름 조회
    @Override
    @Transactional(readOnly = true)
    public String getMemberNameById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return member.getName();
    }

    // 학생ID로 학부모ID 조회
    @Override
    @Transactional(readOnly = true)
    public List<Member> findParentsByStudentId(Long studentId) {
        Member student = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return student.getFollowedList().stream()
                .map(MemberFollow::getFollow) // 학부모 Member 가져오기
                .collect(Collectors.toList());
    }

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = MemberPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // LoginUser를 MemberDto로 변환
    private MemberDto loginUserConvertToMemberDto(LoginUserDto loginUser) {
        return MemberDto.builder()
                .id(loginUser.getId())
                .name(loginUser.getName())
                .profileImageUrl(loginUser.getProfileImageUrl())
                .schoolName(loginUser.getSchoolName())
                .year(loginUser.getYear())
                .classId(loginUser.getClassId())
                .number(loginUser.getNumber())
                .subject(loginUser.getSubject())
                .role(loginUser.getRole())
                .build();
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

    // LoginUser를 DetailMemberDto로 변환
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
