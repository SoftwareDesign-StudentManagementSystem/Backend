package com.iEdu.domain.studentRecord.counsel.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.studentRecord.counsel.dto.req.CounselForm;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselDto;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselPageCacheDto;
import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import com.iEdu.domain.studentRecord.counsel.entity.CounselPage;
import com.iEdu.domain.studentRecord.counsel.repository.CounselQueryRepository;
import com.iEdu.domain.studentRecord.counsel.repository.CounselRepository;
import com.iEdu.domain.studentRecord.counsel.service.CounselService;
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
import java.util.stream.Collectors;

import static com.iEdu.global.common.utils.Converter.convertToSemesterEnum;
import static com.iEdu.global.common.utils.RoleValidator.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounselServiceImpl implements CounselService {
    private final CounselRepository counselRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final CounselQueryRepository counselQueryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // 학생의 모든 상담 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<CounselDto> getAllCounsel(Long studentId, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선), createdAt(내림차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"), Sort.Order.desc("createdAt"))
        );
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        validateAccessToStudent(loginUser, studentId);
        Page<Counsel> counselPage = counselRepository.findByMemberId(studentId, sortedPageable);
        return counselPage.map(this::convertToCounselDto);
    }

    // (학년/반/번호/학기)로 학생들 상담 조회 [선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public List<CounselDto> getStudentsCounsel(Integer year, Integer classId, Integer number, Integer semester, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Semester semesterEnum = convertToSemesterEnum(semester);
        // 학생 목록 조회
        List<Member> students = memberRepository.findStudentsByYearClassNumber(year, classId, number);
        return students.stream()
                .flatMap(student -> counselQueryRepository
                        .findByMemberIdAndYearAndSemester(student.getId(), year, semesterEnum)
                        .stream()
                )
                .map(this::convertToCounselDto)
                .collect(Collectors.toList());
    }

    // (학년/학기)로 학생 상담 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public Page<CounselDto> getFilterCounsel(Long studentId, Integer year, Integer semester, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"))
        );
        validateAccessToStudent(loginUser, studentId);
        Semester semesterEnum = convertToSemesterEnum(semester);
        String roleKey = loginUser.getRole().name();
        String cacheKey = String.format(
                "counsel:%d:%d:%s:%d:%d:%s",
                studentId, year, semesterEnum, pageable.getPageNumber(), pageable.getPageSize(), roleKey
        );
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            JavaType type = objectMapper.getTypeFactory().constructType(CounselPageCacheDto.class);
            CounselPageCacheDto cacheDto = objectMapper.convertValue(cached, type);
            return new PageImpl<>(
                    cacheDto.getContent(),
                    PageRequest.of(cacheDto.getPageNumber(), cacheDto.getPageSize()),
                    cacheDto.getTotalElements()
            );
        }
        Page<Counsel> counselPage = counselRepository.findByMemberIdAndYearAndSemester(studentId, year, semesterEnum, sortedPageable);
        Page<CounselDto> resultPage = counselPage.map(this::convertToCounselDto);
        CounselPageCacheDto cacheDto = CounselPageCacheDto.builder()
                .content(resultPage.getContent())
                .pageNumber(resultPage.getNumber())
                .pageSize(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .build();
        redisTemplate.opsForValue().set(cacheKey, cacheDto, Duration.ofMinutes(10));
        return resultPage;
    }

    // 학생 상담 생성 [선생님 권한]
    @Override
    @Transactional
    public void createCounsel(Long studentId, CounselForm counselForm, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Member student = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Counsel counsel = Counsel.builder()
                .member(student)
                .teacherName(loginUser.getName())
                .year(counselForm.getYear())
                .semester(counselForm.getSemester())
                .date(counselForm.getDate())
                .content(counselForm.getContent())
                .nextCounselDate(counselForm.getNextCounselDate())
                .build();
        counselRepository.save(counsel);

        // 상담 알림 생성 & Kafka 이벤트 생성
        try {
            List<Member> parentList = memberService.findParentsByStudentId(student.getId());
            for (Member parent : parentList) {
                Notification notification = Notification.builder()
                        .receiverId(parent.getId())
                        .objectId(counsel.getId())
                        .content("자녀의 " + counsel.getYear() + "학년 " + counsel.getSemester().toKoreanString() + " 상담내역이 등록되었습니다.")
                        .targetObject(Notification.TargetObject.Counsel)
                        .build();
                kafkaTemplate.send("counsel-topic", objectMapper.writeValueAsString(notification));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Notification: {}", e.getMessage());
        }
    }

    // 학생 상담 수정 [선생님 권한]
    @Override
    @Transactional
    public void updateCounsel(Long counselId, CounselForm counselForm, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Counsel counsel = counselRepository.findById(counselId)
                .orElseThrow(() -> new ServiceException(ReturnCode.COUNSEL_NOT_FOUND));
        counsel.setYear(counselForm.getYear());
        counsel.setSemester(counselForm.getSemester());
        counsel.setDate(counselForm.getDate());
        counsel.setContent(counselForm.getContent());
        counsel.setNextCounselDate(counselForm.getNextCounselDate());
        // 캐시 무효화
        evictCounselCache(counsel);

        // 상담 알림 수정 & Kafka 이벤트 생성
        try {
            List<Member> parentList = memberService.findParentsByStudentId(counsel.getMember().getId());
            for (Member parent : parentList) {
                Notification notification = Notification.builder()
                        .receiverId(parent.getId())
                        .objectId(counsel.getId())
                        .content("자녀의 " + counsel.getYear() + "학년 " + counsel.getSemester().toKoreanString() + " 상담내역이 수정되었습니다.")
                        .targetObject(Notification.TargetObject.Counsel)
                        .build();
                kafkaTemplate.send("counsel-topic", objectMapper.writeValueAsString(notification));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Notification: {}", e.getMessage());
        }
    }

    // 학생 상담 삭제 [선생님 권한]
    @Override
    @Transactional
    public void deleteCounsel(Long counselId, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        validateTeacherRole(loginUser);
        Counsel counsel = counselRepository.findById(counselId)
                .orElseThrow(() -> new ServiceException(ReturnCode.COUNSEL_NOT_FOUND));
        counselRepository.delete(counsel);
        // 캐시 무효화
        evictCounselCache(counsel);
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = CounselPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    public void evictCounselCache(Counsel counsel) {
        Long studentId = counsel.getMember().getId();
        int year = counsel.getYear();
        Semester semester = counsel.getSemester();
        String baseKeyPattern = String.format("counsel:%d:%d:%s*", studentId, year, semester);

        Set<String> keysToDelete = redisTemplate.keys(baseKeyPattern);
        if (keysToDelete != null && !keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
        }
    }

    // Counsel -> CounselDto 변환
    @Override
    public CounselDto convertToCounselDto(Counsel counsel) {
        return CounselDto.builder()
                .id(counsel.getId())
                .studentId(counsel.getMember().getId())
                .teacherName(counsel.getTeacherName())
                .year(counsel.getYear())
                .semester(counsel.getSemester())
                .date(counsel.getDate())
                .content(counsel.getContent())
                .nextCounselDate(counsel.getNextCounselDate())
                .build();
    }
}
