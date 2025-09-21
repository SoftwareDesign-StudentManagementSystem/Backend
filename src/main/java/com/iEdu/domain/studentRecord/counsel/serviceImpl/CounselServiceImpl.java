package com.iEdu.domain.studentRecord.counsel.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.studentRecord.counsel.dto.req.CounselRequest;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselResponse;
import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import com.iEdu.domain.studentRecord.counsel.entity.CounselPage;
import com.iEdu.domain.studentRecord.counsel.repository.CounselQueryRepository;
import com.iEdu.domain.studentRecord.counsel.repository.CounselRepository;
import com.iEdu.domain.studentRecord.counsel.service.CounselService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.response.PageResponse;
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
import java.util.stream.Collectors;

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
    private final RoleValidator roleValidator;
    private final RedisCacheEvictHelper redisCacheEvictHelper;

    // 학생의 모든 상담 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public PageResponse<CounselResponse> getAllCounsel(Long studentId, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateAccessToStudent(loginUser, studentId);
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선), createdAt(내림차순)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"), Sort.Order.desc("createdAt"))
        );
        Page<Counsel> counselPage = counselRepository.findByMemberId(studentId, sortedPageable);
        return PageResponse.of(counselPage.map(this::convertToCounselDto));
    }

    // (학년/반/번호/학기)로 학생들 상담 조회 [선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public List<CounselResponse> getStudentsCounsel(Integer year, Integer classId, Integer number, Semester semester, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        // 학생 목록 조회
        List<Member> students = memberRepository.findStudentsByYearClassNumber(year, classId, number);
        List<Long> studentIds = students.stream()
                .map(Member::getId)
                .toList();
        // studentId -> List<Counsel> 맵핑
        List<Counsel> counselList = counselQueryRepository
                .findByMemberIdInAndYearAndSemester(studentIds, year, semester);
        return counselList.stream()
                .map(this::convertToCounselDto)
                .collect(Collectors.toList());
    }

    // (학년/학기)로 학생 상담 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "counsel",
            key = "'student:' + #studentId + ':' + #year + ':' + #semester + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public PageResponse<CounselResponse> getFilterCounsel(Long studentId, Integer year, Semester semester, Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateAccessToStudent(loginUser, studentId);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"))
        );
        Page<Counsel> counselPage = counselRepository.findByMemberIdAndYearAndSemester(studentId, year, semester, sortedPageable);
        return PageResponse.of(counselPage.map(this::convertToCounselDto));
    }

    // 학생 상담 생성 [선생님 권한]
    @Override
    @Transactional
    public void createCounsel(Long studentId, CounselRequest counselRequest, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Member student = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Counsel counsel = Counsel.builder()
                .member(student)
                .teacherName(loginUser.getName())
                .year(counselRequest.getYear())
                .semester(counselRequest.getSemester())
                .date(counselRequest.getDate())
                .content(counselRequest.getContent())
                .nextCounselDate(counselRequest.getNextCounselDate())
                .build();
        counselRepository.save(counsel);

        // 상담 알림 생성 & Kafka 이벤트 생성
        sendCounselNotification(counsel, "등록");
    }

    // 학생 상담 수정 [선생님 권한]
    @Override
    @Transactional
    public void updateCounsel(Long counselId, CounselRequest counselRequest, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Counsel counsel = counselRepository.findById(counselId)
                .orElseThrow(() -> new ServiceException(ReturnCode.COUNSEL_NOT_FOUND));
        if (counselRequest.getYear() != null) counsel.setYear(counselRequest.getYear());
        if (counselRequest.getSemester() != null) counsel.setSemester(counselRequest.getSemester());
        if (counselRequest.getDate() != null) counsel.setDate(counselRequest.getDate());
        if (counselRequest.getContent() != null) counsel.setContent(counselRequest.getContent());
        if (counselRequest.getNextCounselDate() != null) counsel.setNextCounselDate(counselRequest.getNextCounselDate());
        // 캐시 무효화
        evictCounselCache(counsel.getMember().getId(), counsel.getYear(), counsel.getSemester());
        // 상담 알림 수정 & Kafka 이벤트 생성
        sendCounselNotification(counsel, "수정");
    }

    // 학생 상담 삭제 [선생님 권한]
    @Override
    @Transactional
    public void deleteCounsel(Long counselId, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Counsel counsel = counselRepository.findById(counselId)
                .orElseThrow(() -> new ServiceException(ReturnCode.COUNSEL_NOT_FOUND));
        counselRepository.delete(counsel);
        // 캐시 무효화
        evictCounselCache(counsel.getMember().getId(), counsel.getYear(), counsel.getSemester());
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = CounselPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // 캐시 무효화
    private void evictCounselCache(Long studentId, Integer year, Semester semester) {
        String prefix = "counsel::student:" + studentId + ":" + year + ":" + semester;
        redisCacheEvictHelper.evictByPrefix(prefix);
        log.debug("Counsel cache evicted for studentId={}, year={}, semester={}, prefix={}", studentId, year, semester, prefix);
    }

    // 상담 알림 이벤트 생성
    private void sendCounselNotification(Counsel counsel, String actionMessage) {
        try {
            List<Member> parentList = memberService.findParentsByStudentId(counsel.getMember().getId());
            for (Member parent : parentList) {
                Notification notification = Notification.builder()
                        .receiverId(parent.getId())
                        .objectId(counsel.getId())
                        .content("자녀의 " + counsel.getYear() + "학년 " + counsel.getSemester().toKoreanString() + " 상담내역이 " + actionMessage + "되었습니다.")
                        .targetObject(Notification.TargetObject.Counsel)
                        .build();
                kafkaTemplate.send("counsel-topic", objectMapper.writeValueAsString(notification));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Notification: {}", e.getMessage());
        }
    }

    // Counsel -> CounselDto 변환
    @Override
    public CounselResponse convertToCounselDto(Counsel counsel) {
        return CounselResponse.builder()
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
