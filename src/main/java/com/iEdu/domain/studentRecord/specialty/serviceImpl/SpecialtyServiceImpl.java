package com.iEdu.domain.studentRecord.specialty.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.studentRecord.specialty.dto.req.SpecialtyRequest;
import com.iEdu.domain.studentRecord.specialty.dto.res.SpecialtyResponse;
import com.iEdu.domain.studentRecord.specialty.entity.Specialty;
import com.iEdu.domain.studentRecord.specialty.entity.SpecialtyPage;
import com.iEdu.domain.studentRecord.specialty.repository.SpecialtyRepository;
import com.iEdu.domain.studentRecord.specialty.service.SpecialtyService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.utils.RoleValidator;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import com.iEdu.global.redis.helper.RedisCacheEvictHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpecialtyServiceImpl implements SpecialtyService {
    private final SpecialtyRepository specialtyRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RoleValidator roleValidator;
    private final RedisCacheEvictHelper redisCacheEvictHelper;

    // 학생의 모든 특기사항 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<SpecialtyResponse> getAllSpecialty(Long studentId, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateAccessToStudent(loginUser, studentId);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"), Sort.Order.desc("createdAt"))
        );
        Page<Specialty> specialtyPage = specialtyRepository.findByMemberId(studentId, sortedPageable);
        return specialtyPage.map(this::convertToSpecialtyDto);
    }

    // (학년/학기)로 학생 특기사항 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "specialty",
            key = "'student:' + #studentId + ':' + #year + ':' + #semester + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #loginUser.role.name()"
    )
    public Page<SpecialtyResponse> getFilterSpecialty(Long studentId, Integer year, Semester semester, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        roleValidator.validateAccessToStudent(loginUser, studentId);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Specialty> specialtyPage = specialtyRepository.findByMemberIdAndYearAndSemester(
                studentId, year, semester, sortedPageable
        );
        return specialtyPage.map(this::convertToSpecialtyDto);
    }

    // 학생 특기사항 생성 [선생님 권한]
    @Override
    @Transactional
    public void createSpecialty(Long studentId, SpecialtyRequest specialtyRequest, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Member student = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Specialty specialty = Specialty.builder()
                .member(student)
                .teacherName(loginUser.getName())
                .year(specialtyRequest.getYear())
                .semester(specialtyRequest.getSemester())
                .date(specialtyRequest.getDate())
                .content(specialtyRequest.getContent())
                .build();
        specialtyRepository.save(specialty);

        // 특기사항 알림 생성 & Kafka 이벤트 생성
        sendSpecialtyNotification(specialty, "등록");
    }

    // 학생 특기사항 수정 [선생님 권한]
    @Override
    @Transactional
    public void updateSpecialty(Long specialtyId, SpecialtyRequest specialtyRequest, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ServiceException(ReturnCode.SPECIALTY_NOT_FOUND));
        specialty.setYear(specialtyRequest.getYear());
        specialty.setSemester(specialtyRequest.getSemester());
        specialty.setDate(specialtyRequest.getDate());
        specialty.setContent(specialtyRequest.getContent());

        // 캐시 무효화
        evictSpecialtyCache(specialty.getMember().getId(), specialty.getYear(), specialty.getSemester());
        // 특기사항 알림 수정 & Kafka 이벤트 생성
        sendSpecialtyNotification(specialty, "수정");
    }

    // 학생 특기사항 삭제 [선생님 권한]
    @Override
    @Transactional
    public void deleteSpecialty(Long specialtyId, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ServiceException(ReturnCode.SPECIALTY_NOT_FOUND));
        specialtyRepository.delete(specialty);
        // 캐시 무효화
        evictSpecialtyCache(specialty.getMember().getId(), specialty.getYear(), specialty.getSemester());
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = SpecialtyPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // 캐시 무효화
    private void evictSpecialtyCache(Long studentId, Integer year, Semester semester) {
        String prefix = "student:" + studentId + ":" + year + ":" + semester + ":";
        redisCacheEvictHelper.evictByPrefix(prefix);
        log.debug("Specialty cache evicted for studentId={}, year={}, semester={}, prefix={}", studentId, year, semester, prefix);
    }

    // 특기사항 알림 이벤트 생성
    private void sendSpecialtyNotification(Specialty specialty, String action) {
        try {
            List<Member> parentList = memberService.findParentsByStudentId(specialty.getMember().getId());
            for (Member parent : parentList) {
                String message = String.format(
                        "자녀의 %d학년 %s 특기사항이 %s되었습니다.",
                        specialty.getYear(),
                        specialty.getSemester().toKoreanString(),
                        action
                );
                Notification parentNotification = Notification.builder()
                        .receiverId(parent.getId())
                        .objectId(specialty.getId())
                        .content(message)
                        .targetObject(Notification.TargetObject.Specialty)
                        .build();
                kafkaTemplate.send("specialty-topic", objectMapper.writeValueAsString(parentNotification));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Notification: {}", e.getMessage());
        }
    }

    // Specialty → SpecialtyDto 변환
    @Override
    public SpecialtyResponse convertToSpecialtyDto(Specialty specialty) {
        return SpecialtyResponse.builder()
                .id(specialty.getId())
                .studentId(specialty.getMember().getId())
                .teacherName(specialty.getTeacherName())
                .year(specialty.getYear())
                .semester(specialty.getSemester())
                .date(specialty.getDate())
                .content(specialty.getContent())
                .build();
    }
}
