package com.iEdu.domain.studentRecord.specialty.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.studentRecord.specialty.dto.req.SpecialtyForm;
import com.iEdu.domain.studentRecord.specialty.dto.res.SpecialtyDto;
import com.iEdu.domain.studentRecord.specialty.dto.res.SpecialtyPageCacheDto;
import com.iEdu.domain.studentRecord.specialty.entity.Specialty;
import com.iEdu.domain.studentRecord.specialty.entity.SpecialtyPage;
import com.iEdu.domain.studentRecord.specialty.repository.SpecialtyRepository;
import com.iEdu.domain.studentRecord.specialty.service.SpecialtyService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static com.iEdu.global.common.utils.Converter.convertToSemesterEnum;
import static com.iEdu.global.common.utils.RoleValidator.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpecialtyServiceImpl implements SpecialtyService {
    private final SpecialtyRepository specialtyRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // 학생의 모든 특기사항 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<SpecialtyDto> getAllSpecialty(Long studentId, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"), Sort.Order.desc("createdAt"))
        );
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        validateAccessToStudent(loginUser, studentId);
        Page<Specialty> specialtyPage = specialtyRepository.findByMemberId(studentId, sortedPageable);
        return specialtyPage.map(this::convertToSpecialtyDto);
    }

    // (학년/학기)로 학생 특기사항 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<SpecialtyDto> getFilterSpecialty(Long studentId, Integer year, Integer semester, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        validateAccessToStudent(loginUser, studentId);
        Semester semesterEnum = convertToSemesterEnum(semester);
        String roleKey = loginUser.getRole().name();
        String cacheKey = String.format(
                "specialty:%d:%d:%s:%d:%d:%s",
                studentId, year, semesterEnum, pageable.getPageNumber(), pageable.getPageSize(), roleKey
        );
        // Redis에서 캐시 조회
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            JavaType type = objectMapper.getTypeFactory().constructType(SpecialtyPageCacheDto.class);
            SpecialtyPageCacheDto cacheDto = objectMapper.convertValue(cached, type);
            return new PageImpl<>(
                    cacheDto.getContent(),
                    PageRequest.of(cacheDto.getPageNumber(), cacheDto.getPageSize()),
                    cacheDto.getTotalElements()
            );
        }
        Page<Specialty> specialtyPage = specialtyRepository.findByMemberIdAndYearAndSemester(
                studentId, year, semesterEnum, sortedPageable
        );
        Page<SpecialtyDto> resultPage = specialtyPage.map(this::convertToSpecialtyDto);
        SpecialtyPageCacheDto cacheDto = SpecialtyPageCacheDto.builder()
                .content(resultPage.getContent())
                .pageNumber(resultPage.getNumber())
                .pageSize(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .build();
        redisTemplate.opsForValue().set(cacheKey, cacheDto, Duration.ofMinutes(10));
        return resultPage;
    }

    // 학생 특기사항 생성 [선생님 권한]
    @Override
    @Transactional
    public void createSpecialty(Long studentId, SpecialtyForm specialtyForm, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Member student = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Specialty specialty = Specialty.builder()
                .member(student)
                .teacherName(loginUser.getName())
                .year(specialtyForm.getYear())
                .semester(specialtyForm.getSemester())
                .date(specialtyForm.getDate())
                .content(specialtyForm.getContent())
                .build();
        specialtyRepository.save(specialty);

        // 특기사항 알림 생성 & Kafka 이벤트 생성
        try {
            List<Member> parentList = memberService.findParentsByStudentId(studentId);
            for (Member parent : parentList) {
                Notification parentNotification = Notification.builder()
                        .receiverId(parent.getId())
                        .objectId(specialty.getId())
                        .content("자녀의 " + specialty.getYear() + "학년 " +
                                specialty.getSemester().toKoreanString() + " 특기사항이 등록되었습니다.")
                        .targetObject(Notification.TargetObject.Specialty)
                        .build();
                kafkaTemplate.send("specialty-topic", objectMapper.writeValueAsString(parentNotification));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Notification: {}", e.getMessage());
        }
    }

    // 학생 특기사항 수정 [선생님 권한]
    @Override
    @Transactional
    public void updateSpecialty(Long specialtyId, SpecialtyForm specialtyForm, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ServiceException(ReturnCode.SPECIALTY_NOT_FOUND));
        specialty.setYear(specialtyForm.getYear());
        specialty.setSemester(specialtyForm.getSemester());
        specialty.setDate(specialtyForm.getDate());
        specialty.setContent(specialtyForm.getContent());
        // 캐시 무효화
        evictSpecialtyCache(specialty);

        // 특기사항 알림 수정 & Kafka 이벤트 생성
        try {
            List<Member> parentList = memberService.findParentsByStudentId(specialty.getMember().getId());
            for (Member parent : parentList) {
                Notification parentNotification = Notification.builder()
                        .receiverId(parent.getId())
                        .objectId(specialty.getId())
                        .content("자녀의 " + specialty.getYear() + "학년 " +
                                specialty.getSemester().toKoreanString() + " 특기사항이 수정되었습니다.")
                        .targetObject(Notification.TargetObject.Specialty)
                        .build();
                kafkaTemplate.send("specialty-topic", objectMapper.writeValueAsString(parentNotification));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Notification: {}", e.getMessage());
        }
    }

    // 학생 특기사항 삭제 [선생님 권한]
    @Override
    @Transactional
    public void deleteSpecialty(Long specialtyId, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ServiceException(ReturnCode.SPECIALTY_NOT_FOUND));
        specialtyRepository.delete(specialty);
        // 캐시 무효화
        evictSpecialtyCache(specialty);
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = SpecialtyPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    public void evictSpecialtyCache(Specialty specialty) {
        Long studentId = specialty.getMember().getId();
        int year = specialty.getYear();
        Semester semester = specialty.getSemester();

        // 캐시 키 패턴에 roleKey, pageNumber, pageSize 전부 와일드카드 처리
        String pattern = String.format("specialty:%d:%d:%s:*:*:*", studentId, year, semester.name());
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // Specialty → SpecialtyDto 변환
    @Override
    public SpecialtyDto convertToSpecialtyDto(Specialty specialty) {
        return SpecialtyDto.builder()
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
