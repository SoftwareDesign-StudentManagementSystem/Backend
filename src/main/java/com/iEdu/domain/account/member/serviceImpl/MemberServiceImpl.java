package com.iEdu.domain.account.member.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.account.auth.service.AuthService;
import com.iEdu.domain.account.member.dto.req.BasicUpdateRequest;
import com.iEdu.domain.account.member.dto.req.FollowRequest;
import com.iEdu.domain.account.member.dto.req.ParentSignUpRequest;
import com.iEdu.domain.account.member.dto.req.TeacherUpdateRequest;
import com.iEdu.domain.account.member.dto.res.DetailMemberResponse;
import com.iEdu.domain.account.member.dto.res.MemberResponse;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
import com.iEdu.domain.account.member.entity.MemberFollowReq;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.account.member.mapper.MemberMapper;
import com.iEdu.domain.account.member.repository.MemberFollowRepository;
import com.iEdu.domain.account.member.repository.MemberFollowReqRepository;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.account.member.entity.QMember;
import com.iEdu.global.common.response.PageResponse;
import com.iEdu.global.common.utils.RoleValidator;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import com.iEdu.global.redis.helper.RedisCacheEvictHelper;
import com.iEdu.global.s3.S3Service;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final MemberMapper memberMapper;
    private final RoleValidator roleValidator;
    @Autowired
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final RedisCacheEvictHelper redisCacheEvictHelper;

    // 학부모 회원가입
    @Override
    @Transactional
    public Member signup(ParentSignUpRequest parentSignUpRequest){
        Long accountId = parentSignUpRequest.getAccountId();
        String email = parentSignUpRequest.getEmail();
        // 0. accountId 길이 검증 (11자리인지 확인)
        if (String.valueOf(accountId).length() != 11) {
            log.error("잘못된 parentAccountId: {}", accountId);
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
        String encodedPassword = parentSignUpRequest.getPassword() != null ? passwordEncoder.encode(parentSignUpRequest.getPassword()) : null;
        Member member = Member.builder()
                .accountId(parentSignUpRequest.getAccountId())
                .password(encodedPassword)
                .name(parentSignUpRequest.getName())
                .phone(parentSignUpRequest.getPhone())
                .email(parentSignUpRequest.getEmail())
                .birthday(parentSignUpRequest.getBirthday())
                .schoolName(parentSignUpRequest.getSchoolName())
                .gender(parentSignUpRequest.getGender())
                .build();
        memberRepository.save(member);
        return member;
    }

    // 본인 회원정보 조회
    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMyInfo(CurrentUserDto currentUser) {
        return memberMapper.toMemberResponse(currentUser);
    }

    // 본인 상세회원정보 조회
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "member", key = "'myDetailInfo:' + #currentUser.role.name() + ':' + #currentUser.id")
    public DetailMemberResponse getMyDetailInfo(CurrentUserDto currentUser) {
        Member member = memberRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberMapper.toDetailMemberResponse(member);
    }

    // 담당 학생들의 회원정보 조회 [선생님 권한]
    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "member",
            key = "'myStudents:' + #currentUser.role.name() + ':' + #currentUser.id + ':' + #currentUser.classId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public PageResponse<MemberResponse> getMyStudentInfo(Pageable pageable, CurrentUserDto currentUser) {
        checkPageSize(pageable.getPageSize());
        roleValidator.validateTeacherRole(currentUser);

        Integer year = currentUser.getYear();
        Integer classId = currentUser.getClassId();
        if (classId == null) {
            throw new ServiceException(ReturnCode.CLASSID_NOT_FOUND);
        }
        Page<Member> students = memberRepository.findAllByYearAndClassIdAndRole(
                year, classId, Member.MemberRole.ROLE_STUDENT, pageable
        );
        return PageResponse.of(students.map(memberMapper::toMemberResponse));
    }

    // (학년/반/번호)로 학생 조회 [선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> getMyFilterInfo(Integer year, Integer classId, Integer number, Pageable pageable, CurrentUserDto currentUser){
        checkPageSize(pageable.getPageSize());
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(currentUser);
        QMember member = QMember.member;
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(member.role.eq(Member.MemberRole.ROLE_STUDENT));
        if (year != null) {
            builder.and(member.year.eq(year));
        }
        if (classId != null) {
            builder.and(member.classId.eq(classId));
        }
        if (number != null) {
            builder.and(member.number.eq(number));
        }
        Page<Member> memberPage = memberRepository.findAll(builder, pageable);
        return PageResponse.of(memberPage.map(memberMapper::toMemberResponse));
    }

    // 학생의 회원정보 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMemberInfo(Long studentId, CurrentUserDto currentUser) {
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateAccessToStudent(currentUser, studentId);
        Member student = memberRepository.findByIdAndRole(studentId, Member.MemberRole.ROLE_STUDENT)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberMapper.toMemberResponse(student);
    }

    // 학생의 상세회원정보 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public DetailMemberResponse getMemberDetailInfo(Long studentId, CurrentUserDto currentUser) {
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateAccessToStudent(currentUser, studentId);
        Member student = memberRepository.findByIdAndRole(studentId, Member.MemberRole.ROLE_STUDENT)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberMapper.toDetailMemberResponse(student);
    }

    // 학생/학부모 회원정보 수정 [학생/학부모 권한]
    @Override
    @Transactional
    public void basicUpdateMemberInfo(BasicUpdateRequest basicUpdateRequest, MultipartFile imageFile, CurrentUserDto currentUser){
        // ROLE_STUDENT/ROLE_PARENT 아닌 경우 예외 처리
        roleValidator.validateStudentOrParentRole(currentUser);
        // 기존 이미지 삭제 후 입력 받은 이미지 S3에 저장
        String imageUrl = currentUser.getProfileImageUrl(); // 기본적으로 기존 이미지 URL을 사용
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 없으면 바로 새로운 이미지 저장
            if (imageUrl != null && !imageUrl.isEmpty()) s3Service.deleteFile(imageUrl);
            try {
                imageUrl = s3Service.uploadImageFile(imageFile, "profile-image");
            } catch (IOException e) {
                throw new ServiceException(ReturnCode.INTERNAL_ERROR);
            }
        } else {
            // imageFile이 없으면 기존 이미지가 있다면 삭제한다
            if (imageUrl != null && !imageUrl.isEmpty()) s3Service.deleteFile(imageUrl); // 기존 이미지 삭제
            imageUrl = null;
        }
        if (basicUpdateRequest.getPassword() != null) {
            currentUser.setPassword(BCrypt.hashpw(basicUpdateRequest.getPassword(), BCrypt.gensalt()));
        }
        if (basicUpdateRequest.getName() != null) currentUser.setName(basicUpdateRequest.getName());
        if (basicUpdateRequest.getBirthday() != null) currentUser.setBirthday(basicUpdateRequest.getBirthday());
        if (basicUpdateRequest.getSchoolName() != null) currentUser.setSchoolName(basicUpdateRequest.getSchoolName());
        if (basicUpdateRequest.getGender() != null) currentUser.setGender(basicUpdateRequest.getGender());
        if (basicUpdateRequest.getPhone() != null) currentUser.setPhone(basicUpdateRequest.getPhone());
        if (basicUpdateRequest.getEmail() != null) currentUser.setEmail(basicUpdateRequest.getEmail());
        currentUser.setProfileImageUrl(imageUrl);
        // LoginUserDto를 Member 엔티티로 변환
        Member memberEntity = memberMapper.toMember(currentUser);
        memberRepository.save(memberEntity);
        // 상세회원정보 캐시 무효화
        evictMyDetailCache(currentUser.getId(), currentUser.getRole());
        // 학생이라면 담임 선생님 캐시 무효화
        if (currentUser.getRole() == Member.MemberRole.ROLE_STUDENT) {
            evictTeacherStudentCache(currentUser.getYear(), currentUser.getClassId());
        }
    }

    // 선생님 회원정보 수정 [선생님 권한]
    @Override
    @Transactional
    public void teacherUpdateMemberInfo(TeacherUpdateRequest teacherUpdateRequest, MultipartFile imageFile, CurrentUserDto currentUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(currentUser);
        // 기존 이미지 삭제 후 입력 받은 이미지 S3에 저장
        String imageUrl = currentUser.getProfileImageUrl(); // 기본적으로 기존 이미지 URL을 사용
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 없으면 바로 새로운 이미지 저장
            if (imageUrl != null && !imageUrl.isEmpty()) s3Service.deleteFile(imageUrl);
            try {
                imageUrl = s3Service.uploadImageFile(imageFile, "profile-image");
            } catch (IOException e) {
                throw new ServiceException(ReturnCode.INTERNAL_ERROR);
            }
        } else {
            // imageFile이 없으면 기존 이미지가 있다면 삭제한다
            if (imageUrl != null && !imageUrl.isEmpty()) s3Service.deleteFile(imageUrl); // 기존 이미지 삭제
            imageUrl = null;
        }
        if (teacherUpdateRequest.getPassword() != null) {
            currentUser.setPassword(BCrypt.hashpw(teacherUpdateRequest.getPassword(), BCrypt.gensalt()));
        }
        if (teacherUpdateRequest.getName() != null) currentUser.setName(teacherUpdateRequest.getName());
        if (teacherUpdateRequest.getBirthday() != null) currentUser.setBirthday(teacherUpdateRequest.getBirthday());
        if (teacherUpdateRequest.getSchoolName() != null) currentUser.setSchoolName(teacherUpdateRequest.getSchoolName());
        if (teacherUpdateRequest.getYear() != null) currentUser.setYear(teacherUpdateRequest.getYear());
        if (teacherUpdateRequest.getClassId() != null) currentUser.setClassId(teacherUpdateRequest.getClassId());
        if (teacherUpdateRequest.getSubject() != null) currentUser.setSubject(teacherUpdateRequest.getSubject());
        if (teacherUpdateRequest.getGender() != null) currentUser.setGender(teacherUpdateRequest.getGender());
        if (teacherUpdateRequest.getPhone() != null) currentUser.setPhone(teacherUpdateRequest.getPhone());
        if (teacherUpdateRequest.getEmail() != null) currentUser.setEmail(teacherUpdateRequest.getEmail());
        currentUser.setProfileImageUrl(imageUrl);
        // LoginUserDto를 Member 엔티티로 변환
        Member memberEntity = memberMapper.toMember(currentUser);
        memberRepository.save(memberEntity);
        // 상세회원정보 캐시 무효화
        evictMyDetailCache(currentUser.getId(), currentUser.getRole());
    }

    // 회원탈퇴
    @Override
    @Transactional
    public void deleteMember(CurrentUserDto currentUser) {
        // refreshToken 삭제
        authService.logout(currentUser);
        // DB에서 회원 조회
        Member memberEntity = memberRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        // ----- 연관된 데이터 삭제 -----
        memberRepository.delete(memberEntity);
        // 상세회원정보 캐시 무효화
        evictMyDetailCache(currentUser.getId(), currentUser.getRole());
    }

    // (학번/이름)으로 학생 검색하기 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> searchMemberInfo(Pageable pageable, String keyword, CurrentUserDto currentUser) {
        // ROLE_PARENT이/ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateParentOrTeacherRole(currentUser);
        checkPageSize(pageable.getPageSize());
        Page<Member> members = memberRepository.findByKeywordAndRole(pageable, keyword, Member.MemberRole.ROLE_STUDENT);
        return PageResponse.of(members.map(memberMapper::toMemberResponse));
    }

    // 팔로우 요청하기 [학부모 권한]
    @Override
    @Transactional
    public void followReq(FollowRequest followRequest, CurrentUserDto currentUser){
        // ROLE_PARENT 아닌 경우 예외 처리
        roleValidator.validateParentRole(currentUser);
        Member followReq = memberMapper.toMember(currentUser);
        Member followRec = memberRepository.findByNameAndYearAndClassIdAndNumberAndBirthday(
                followRequest.getName(),
                followRequest.getYear(),
                followRequest.getClassId(),
                followRequest.getNumber(),
                String.valueOf(followRequest.getBirthday())
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
                .content(currentUser.getName() + " 학부모님이 팔로우를 요청하였습니다.")
                .targetObject(Notification.TargetObject.Follow)
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
    public void cancelFollowReq(Long memberId, CurrentUserDto currentUser){
        // ROLE_PARENT 아닌 경우 예외 처리
        roleValidator.validateParentRole(currentUser);
        Member followReq = memberMapper.toMember(currentUser);
        Member followRec = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        MemberFollowReq memberFollowReq = memberFollowReqRepository.findByFollowReqAndFollowRec(followReq, followRec)
                .orElseThrow(() -> new ServiceException(ReturnCode.REQUEST_NOT_FOUND));
        memberFollowReqRepository.delete(memberFollowReq);
    }

    // 팔로우 요청 수락하기 [학생 권한]
    @Override
    @Transactional
    public void acceptFollowReq(Long memberId, CurrentUserDto currentUser){
        // ROLE_STUDENT 아닌 경우 예외 처리
        roleValidator.validateStudentRole(currentUser);
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Member receiver = memberMapper.toMember(currentUser);
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
                .content(currentUser.getName() + " 학생이 팔로우 요청을 수락하였습니다.")
                .targetObject(Notification.TargetObject.Follow)
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
    public void refuseFollowReq(Long memberId, CurrentUserDto currentUser){
        // ROLE_STUDENT 아닌 경우 예외 처리
        roleValidator.validateStudentRole(currentUser);
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Member receiver = memberMapper.toMember(currentUser);
        MemberFollowReq memberFollowReq = memberFollowReqRepository.findByFollowReqAndFollowRec(requester, receiver)
                .orElseThrow(() -> new ServiceException(ReturnCode.REQUEST_NOT_FOUND));
        memberFollowReqRepository.delete(memberFollowReq);
    }

    // 팔로우 취소하기 [학부모 권한]
    @Override
    @Transactional
    public void cancelFollow(Long memberId, CurrentUserDto currentUser){
        // ROLE_PARENT 아닌 경우 예외 처리
        roleValidator.validateParentRole(currentUser);
        Member follow = memberMapper.toMember(currentUser);
        Member followed = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        MemberFollow memberFollow = memberFollowRepository.findByFollowAndFollowed(follow, followed)
                .orElseThrow(() -> new ServiceException(ReturnCode.FOLLOW_NOT_FOUND));
        memberFollowRepository.delete(memberFollow);
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

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = MemberPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // 본인 캐시 무효화
    private void evictMyDetailCache(Long memberId, Member.MemberRole role) {
        String key = "myDetailInfo:" + role.name() + ":" + memberId;
        cacheManager.getCache("member").evictIfPresent(key);
        log.debug("Evicted detail cache: {}", key);
    }

    // 본인 담임교사 캐시 무효화
    private void evictTeacherStudentCache(Integer year, Integer classId) {
        if (year == null || classId == null) return;
        memberRepository.findByYearAndClassIdAndRole(year, classId, Member.MemberRole.ROLE_TEACHER)
                .ifPresent(teacher -> {
                    // getMyStudentInfo 키는 pageable 포함 → prefix 단위로 날리기
                    String prefix = "member::myStudents:ROLE_TEACHER:" + teacher.getId() + ":" + classId;
                    // prefix 기반으로 캐시 삭제
                    redisCacheEvictHelper.evictByPrefix(prefix);
                    log.debug("Evicted student list cache for teacherId={}, classId={}, prefix={}", teacher.getId(), classId, prefix);
                });
    }
}
