package com.iEdu.domain.account.member.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.auth.service.AuthService;
import com.iEdu.domain.account.member.dto.req.BasicUpdateForm;
import com.iEdu.domain.account.member.dto.req.FollowForm;
import com.iEdu.domain.account.member.dto.req.ParentForm;
import com.iEdu.domain.account.member.dto.req.TeacherUpdateForm;
import com.iEdu.domain.account.member.dto.res.DetailMemberDto;
import com.iEdu.domain.account.member.dto.res.MemberDto;
import com.iEdu.domain.account.member.dto.res.MemberPageCacheDto;
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
import com.iEdu.global.common.utils.RoleValidator;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import com.iEdu.global.s3.S3Service;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.iEdu.global.common.utils.RoleValidator.*;

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
    private final RedisTemplate<String, Object> redisTemplate;
    private final MemberMapper memberMapper;
    private final RoleValidator roleValidator;
    @Autowired
    private final ObjectMapper objectMapper;

    // 학부모 회원가입
    @Override
    @Transactional
    public Member signup(ParentForm parentForm){
        Long accountId = parentForm.getAccountId();
        String email = parentForm.getEmail();
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
        return memberMapper.toMemberDto(loginUser);
    }

    // 본인 상세회원정보 조회
    @Override
    @Transactional
    public DetailMemberDto getMyDetailInfo(LoginUserDto loginUser){
        Long memberId = loginUser.getId();
        String cacheKey = "myDetailInfo::" + memberId;

        // Redis 조회
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        Object cached = ops.get(cacheKey);
        if (cached != null && cached instanceof DetailMemberDto) {
            return (DetailMemberDto) cached;
        }
        DetailMemberDto dto = memberMapper.toDetailMemberDto(loginUser);
        // Redis 저장 (10분 TTL)
        ops.set(cacheKey, dto, 10, TimeUnit.MINUTES);
        return dto;
    }

    // 담당 학생들의 회원정보 조회 [선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<MemberDto> getMyStudentInfo(Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);

        Integer year = loginUser.getYear();
        Integer classId = loginUser.getClassId();
        Long teacherId = loginUser.getId();
        if (classId == null) {
            throw new ServiceException(ReturnCode.CLASSID_NOT_FOUND);
        }
        String cacheKey = String.format("myStudents:%d:%d:%d:%d",
                teacherId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                classId
        );
        // Redis 캐시 조회
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            JavaType type = objectMapper.getTypeFactory().constructType(MemberPageCacheDto.class);
            MemberPageCacheDto cacheDto = objectMapper.convertValue(cached, type);
            return new PageImpl<>(
                    cacheDto.getContent(),
                    PageRequest.of(cacheDto.getPageNumber(), cacheDto.getPageSize()),
                    cacheDto.getTotalElements()
            );
        }
        // DB에서 조회
        Page<Member> students = memberRepository.findAllByYearAndClassIdAndRole(
                year, classId, Member.MemberRole.ROLE_STUDENT, pageable
        );
        Page<MemberDto> dtoPage = students.map(memberMapper::toMemberDto);
        // 캐시에 저장
        MemberPageCacheDto cacheDto = MemberPageCacheDto.builder()
                .content(dtoPage.getContent())
                .pageNumber(dtoPage.getNumber())
                .pageSize(dtoPage.getSize())
                .totalElements(dtoPage.getTotalElements())
                .build();
        redisTemplate.opsForValue().set(cacheKey, cacheDto, Duration.ofMinutes(10));
        return dtoPage;
    }

    // (학년/반/번호)로 학생 조회 [선생님 권한]
    @Override
    @Transactional
    public Page<MemberDto> getMyFilterInfo(Integer year, Integer classId, Integer number, Pageable pageable, LoginUserDto loginUser){
        checkPageSize(pageable.getPageSize());
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
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
        return memberPage.map(memberMapper::toMemberDto);
    }

    // 학생의 회원정보 조회 [학부모/선생님 권한]
    @Override
    @Transactional
    public MemberDto getMemberInfo(Long studentId, LoginUserDto loginUser) {
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateAccessToStudent(loginUser, studentId);
        Member student = memberRepository.findByIdAndRole(studentId, Member.MemberRole.ROLE_STUDENT)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberMapper.toMemberDto(student);
    }

    // 학생의 상세회원정보 조회 [학부모/선생님 권한]
    @Override
    @Transactional
    public DetailMemberDto getMemberDetailInfo(Long studentId, LoginUserDto loginUser) {
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateAccessToStudent(loginUser, studentId);
        Member student = memberRepository.findByIdAndRole(studentId, Member.MemberRole.ROLE_STUDENT)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberMapper.toDetailMemberDto(student);
    }

    // 학생/학부모 회원정보 수정 [학생/학부모 권한]
    @Override
    @Transactional
    public void basicUpdateMemberInfo(BasicUpdateForm basicUpdateForm, MultipartFile imageFile, LoginUserDto loginUser){
        // ROLE_STUDENT/ROLE_PARENT 아닌 경우 예외 처리
        roleValidator.validateStudentOrParentRole(loginUser);
        // 기존 이미지 삭제 후 입력 받은 이미지 S3에 저장
        String imageUrl = loginUser.getProfileImageUrl(); // 기본적으로 기존 이미지 URL을 사용
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 없으면 바로 새로운 이미지 저장
            if (imageUrl != null && !imageUrl.isEmpty()) {
                s3Service.deleteFile(imageUrl);
            }
            try {
                imageUrl = s3Service.uploadImageFile(imageFile, "profile-image");
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
        Member memberEntity = memberMapper.toMember(loginUser);
        memberRepository.save(memberEntity);
        // 캐시 무효화 1: 본인 상세정보 캐시 삭제
        String myDetailCacheKey = "myDetailInfo::" + loginUser.getId();
        redisTemplate.delete(myDetailCacheKey);
        // 캐시 무효화 2: 담임 선생님의 학생 목록 캐시 삭제
        Integer studentYear = loginUser.getYear();
        Integer studentClassId = loginUser.getClassId();
        if (studentYear != null && studentClassId != null) {
            // 해당 연도, 반, 그리고 ROLE_TEACHER인 선생님 1명 조회
            Member teacher = memberRepository.findByYearAndClassIdAndRole(studentYear, studentClassId, Member.MemberRole.ROLE_TEACHER)
                    .orElse(null);
            if (teacher != null) {
                String keyPattern = "myStudents::" + teacher.getId() + "::page::*";
                Set<String> keys = redisTemplate.keys(keyPattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            }
        }
    }

    // 선생님 회원정보 수정 [선생님 권한]
    @Override
    @Transactional
    public void teacherUpdateMemberInfo(TeacherUpdateForm teacherUpdateForm, MultipartFile imageFile, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        // 기존 이미지 삭제 후 입력 받은 이미지 S3에 저장
        String imageUrl = loginUser.getProfileImageUrl(); // 기본적으로 기존 이미지 URL을 사용
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 없으면 바로 새로운 이미지 저장
            if (imageUrl != null && !imageUrl.isEmpty()) {
                s3Service.deleteFile(imageUrl);
            }
            try {
                imageUrl = s3Service.uploadImageFile(imageFile, "profile-image");
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
        Member memberEntity = memberMapper.toMember(loginUser);
        memberRepository.save(memberEntity);
        // 캐시 무효화 1: 본인 상세정보 캐시 삭제
        String myDetailCacheKey = "myDetailInfo::" + loginUser.getId();
        redisTemplate.delete(myDetailCacheKey);
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
        // ROLE_PARENT이/ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateParentOrTeacherRole(loginUser);
        checkPageSize(pageable.getPageSize());
        Page<Member> members = memberRepository.findByKeywordAndRole(pageable, keyword, Member.MemberRole.ROLE_STUDENT);
        return members.map(memberMapper::toMemberDto);
    }

    // 팔로우 요청하기 [학부모 권한]
    @Override
    @Transactional
    public void followReq(FollowForm followForm, LoginUserDto loginUser){
        // ROLE_PARENT 아닌 경우 예외 처리
        roleValidator.validateParentRole(loginUser);
        Member followReq = memberMapper.toMember(loginUser);
        Member followRec = memberRepository.findByNameAndYearAndClassIdAndNumberAndBirthday(
                followForm.getName(),
                followForm.getYear(),
                followForm.getClassId(),
                followForm.getNumber(),
                String.valueOf(followForm.getBirthday())
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
                .content(loginUser.getName() + " 학부모님이 팔로우를 요청하였습니다.")
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
    public void cancelFollowReq(Long memberId, LoginUserDto loginUser){
        // ROLE_PARENT 아닌 경우 예외 처리
        roleValidator.validateParentRole(loginUser);
        Member followReq = memberMapper.toMember(loginUser);
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
        roleValidator.validateStudentRole(loginUser);
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Member receiver = memberMapper.toMember(loginUser);
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
                .content(loginUser.getName() + " 학생이 팔로우 요청을 수락하였습니다.")
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
    public void refuseFollowReq(Long memberId, LoginUserDto loginUser){
        // ROLE_STUDENT 아닌 경우 예외 처리
        roleValidator.validateStudentRole(loginUser);
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Member receiver = memberMapper.toMember(loginUser);
        MemberFollowReq memberFollowReq = memberFollowReqRepository.findByFollowReqAndFollowRec(requester, receiver)
                .orElseThrow(() -> new ServiceException(ReturnCode.REQUEST_NOT_FOUND));
        memberFollowReqRepository.delete(memberFollowReq);
    }

    // 팔로우 취소하기 [학부모 권한]
    @Override
    @Transactional
    public void cancelFollow(Long memberId, LoginUserDto loginUser){
        // ROLE_PARENT 아닌 경우 예외 처리
        roleValidator.validateParentRole(loginUser);
        Member follow = memberMapper.toMember(loginUser);
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
}
