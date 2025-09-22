package com.iEdu.domain.studentRecord.grade.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeRequest;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeUpdateRequest;
import com.iEdu.domain.studentRecord.grade.dto.res.GradeResponse;
import com.iEdu.domain.studentRecord.grade.dto.res.SubjectScoreDto;
import com.iEdu.domain.studentRecord.grade.entity.Grade;
import com.iEdu.domain.studentRecord.grade.entity.GradePage;
import com.iEdu.domain.studentRecord.grade.repository.GradeQueryRepository;
import com.iEdu.domain.studentRecord.grade.repository.GradeRepository;
import com.iEdu.domain.studentRecord.grade.service.GradeService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.response.PageResponse;
import com.iEdu.global.common.utils.RoleValidator;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {
    private final GradeRepository gradeRepository;
    private final MemberRepository memberRepository;
    private final GradeQueryRepository gradeQueryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MemberService memberService;
    private final RoleValidator roleValidator;
    private final CacheManager cacheManager;

    // 본인의 모든 성적 조회 [학생 권한]
    @Override
    @Transactional(readOnly = true)
    public PageResponse<GradeResponse> getMyAllGrade(Pageable pageable, LoginUserDto loginUser){
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"))
        );
        // ROLE_STUDENT 아닌 경우 예외 처리
        roleValidator.validateStudentRole(loginUser);
        Page<Grade> gradePage = gradeRepository.findAllByMemberId(loginUser.getId(), sortedPageable);
        // 각 성적의 year/semester 조합별로 성적 리스트 미리 조회
        Map<String, List<Grade>> gradeMap = preloadAllGrades(gradePage.getContent());
        return PageResponse.of(gradePage.map(grade ->
                convertToGradeDto(grade, loginUser.getAccountId(),
                        gradeMap.get(grade.getYear() + ":" + grade.getSemester()))
        ));
    }

    // 학생의 모든 성적 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public PageResponse<GradeResponse> getAllGrade(Long studentId, Pageable pageable, LoginUserDto loginUser){
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"))
        );
        Member student = memberRepository.getById(studentId);
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateAccessToStudent(loginUser, studentId);
        Page<Grade> gradePage = gradeRepository.findAllByMemberId(studentId, sortedPageable);
        // 각 성적의 year/semester 조합별로 성적 리스트 미리 조회
        Map<String, List<Grade>> gradeMap = preloadAllGrades(gradePage.getContent());
        return PageResponse.of(gradePage.map(grade ->
                convertToGradeDto(grade, student.getAccountId(),
                        gradeMap.get(grade.getYear() + ":" + grade.getSemester()))
        ));
    }

    // (학년/학기)로 본인 성적 조회 [학생 권한]
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "grade", key = "#loginUser.id + ':' + #year + ':' + #semester")
    public GradeResponse getMyFilterGrade(Integer year, Semester semester, LoginUserDto loginUser){
        roleValidator.validateStudentRole(loginUser);
        Grade grade = gradeRepository
                .findByMemberIdAndYearAndSemester(loginUser.getId(), year, semester)
                .orElseThrow(() -> new ServiceException(ReturnCode.GRADE_NOT_FOUND));
        List<Grade> allGrades = gradeRepository.findAllByYearAndSemesterWithMember(year, semester);
        return convertToGradeDto(grade, loginUser.getAccountId(), allGrades);
    }

    // (학년/학기)로 학생 성적 조회 [학부모/선생님 권한]
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "grade", key = "#studentId + ':' + #year + ':' + #semester")
    public GradeResponse getFilterGrade(Long studentId, Integer year, Semester semester, LoginUserDto loginUser){
        roleValidator.validateAccessToStudent(loginUser, studentId);
        Member student = memberRepository.getById(studentId);
        Grade grade = gradeRepository
                .findByMemberIdAndYearAndSemester(studentId, year, semester)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        List<Grade> allGrades = gradeRepository.findAllByYearAndSemesterWithMember(year, semester);
        return convertToGradeDto(grade, student.getAccountId(), allGrades);
    }

    // (학년/반/번호/학기)로 학생들 성적 조회 [선생님 권한]
    @Override
    @Transactional(readOnly = true)
    public List<GradeResponse> getStudentsGrade(Integer year, Integer classId, Integer number, Semester semester, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        List<Grade> grades = gradeQueryRepository.findAllByStudentInfoAndSemesterAndYearWithMember(
                year, classId, number, semester
        );
        // 학년/학기 전체 성적 미리 조회
        List<Grade> allGrades = gradeRepository.findAllByYearAndSemesterWithMember(year, semester);
        // 학급 전체 성적 데이터 기준으로 랭크 계산
        return grades.stream()
                .sorted(Comparator.comparing(g -> g.getMember().getId()))
                .map(grade -> convertToGradeDto(grade, grade.getMember().getAccountId(), allGrades))
                .toList();
    }

    // 학생 성적 생성 [선생님 권한]
    @Override
    @Transactional
    public void createGrade(Long studentId, GradeRequest gradeRequest, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Member student = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        // 선생님 담당 과목 확인
        Member.Subject subject = loginUser.getSubject();
        if (subject == null) throw new ServiceException(ReturnCode.INVALID_SUBJECT);
        Integer year = gradeRequest.getYear();
        Semester semester = gradeRequest.getSemester();
        Double score = gradeRequest.getScore();
        // 기존 성적 존재 여부 확인
        Grade grade = gradeRepository.findByMemberAndYearAndSemester(student, year, semester)
                .orElseGet(() -> Grade.builder()
                        .member(student)
                        .year(year)
                        .semester(semester)
                        .build());
        // 과목별 점수 입력
        updateSubjectScore(grade, subject, score);
        gradeRepository.save(grade);
        // 모든 과목 입력이 완료된 경우에만 알림 발송
        if (isAllSubjectsFilled(grade)) {
            sendGradeNotifications(grade, studentId, year, semester, subject, true);
        }
    }

    // 학생 성적 수정 [선생님 권한]
    @Override
    @Transactional
    public void updateGrade(Long gradeId, GradeUpdateRequest gradeUpdateRequest, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ServiceException(ReturnCode.GRADE_NOT_FOUND));
        // 선생님 담당 과목 확인
        Member.Subject subject = loginUser.getSubject();
        if (subject == null) throw new ServiceException(ReturnCode.INVALID_SUBJECT);

        // 과목별 점수 입력
        updateSubjectScore(grade, subject, gradeUpdateRequest.getScore());
        // 캐시 무효화
        evictGradeCache(grade.getMember().getId(), grade.getYear(), grade.getSemester());
        // 성적 알림 생성 & Kafka 이벤트 생성
        sendGradeNotifications(grade, grade.getMember().getId(), grade.getYear(), grade.getSemester(), subject, false);
    }

    // 학생 성적 삭제 [선생님 권한]
    @Override
    @Transactional
    public void deleteGrade(Long gradeId, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        roleValidator.validateTeacherRole(loginUser);
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ServiceException(ReturnCode.GRADE_NOT_FOUND));
        // 선생님 담당 과목 확인
        Member.Subject subject = loginUser.getSubject();
        if (subject == null) throw new ServiceException(ReturnCode.INVALID_SUBJECT);

        // 담당 과목 점수만 null로 설정 (실제 "삭제" 대신)
        updateSubjectScore(grade, subject, null);
        // 캐시 무효화
        evictGradeCache(grade.getMember().getId(), grade.getYear(), grade.getSemester());
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = GradePage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // 캐시 무효화
    private void evictGradeCache(Long studentId, Integer year, Semester semester) {
        String cacheKey = studentId + ":" + year + ":" + semester;
        cacheManager.getCache("grade").evictIfPresent(cacheKey);
        log.debug("Grade cache evicted: {}", cacheKey);
    }

    // 성적 생성/수정/삭제 매핑
    private void updateSubjectScore(Grade grade, Member.Subject subject, Double score) {
        switch (subject) {
            case 국어 -> grade.setKoreanLanguageScore(score);
            case 수학 -> grade.setMathematicsScore(score);
            case 영어 -> grade.setEnglishScore(score);
            case 사회 -> grade.setSocialStudiesScore(score);
            case 한국사 -> grade.setHistoryScore(score);
            case 윤리 -> grade.setEthicsScore(score);
            case 경제 -> grade.setEconomicsScore(score);
            case 물리 -> grade.setPhysicsScore(score);
            case 화학 -> grade.setChemistryScore(score);
            case 생명과학 -> grade.setBiologyScore(score);
            case 지구과학 -> grade.setEarthScienceScore(score);
            case 음악 -> grade.setMusicScore(score);
            case 미술 -> grade.setArtScore(score);
            case 체육 -> grade.setPhysicalEducationScore(score);
            case 기술가정 -> grade.setTechnologyAndHomeEconomicScore(score);
            case 컴퓨터 -> grade.setComputerScienceScore(score);
            case 제2외국어 -> grade.setSecondForeignLanguageScore(score);
            default -> throw new ServiceException(ReturnCode.INVALID_SUBJECT);
        }
    }

    // 성적 알림 이벤트 생성
    private void sendGradeNotifications(Grade grade, Long studentId, Integer year, Semester semester, Member.Subject subject, boolean created) {
        try {
            String content = created
                    ? year + "학년 " + semester.toKoreanString() + " 성적이 등록되었습니다."
                    : year + "학년 " + semester.toKoreanString() + " " + subject + " 점수가 수정되었습니다.";

            Notification studentNotification = Notification.builder()
                    .receiverId(studentId)
                    .objectId(grade.getId())
                    .content(content)
                    .targetObject(Notification.TargetObject.Grade)
                    .build();
            kafkaTemplate.send("grade-topic", objectMapper.writeValueAsString(studentNotification));

            List<Member> parentList = memberService.findParentsByStudentId(studentId);
            for (Member parent : parentList) {
                Notification parentNotification = Notification.builder()
                        .receiverId(parent.getId())
                        .objectId(grade.getId())
                        .content(created
                                ? "자녀의 " + year + "학년 " + semester.toKoreanString() + " 성적이 등록되었습니다."
                                : "자녀의 " + year + "학년 " + semester.toKoreanString() + " " + subject + " 점수가 수정되었습니다.")
                        .targetObject(Notification.TargetObject.Grade)
                        .build();
                kafkaTemplate.send("grade-topic", objectMapper.writeValueAsString(parentNotification));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Notification: {}", e.getMessage());
        }
    }

    // 모든 과목의 성적 입력 확인
    private boolean isAllSubjectsFilled(Grade grade) {
        return grade.getKoreanLanguageScore() != null
                && grade.getMathematicsScore() != null
                && grade.getEnglishScore() != null
                && grade.getSocialStudiesScore() != null
                && grade.getHistoryScore() != null
                && grade.getEthicsScore() != null
                && grade.getEconomicsScore() != null
                && grade.getPhysicsScore() != null
                && grade.getChemistryScore() != null
                && grade.getBiologyScore() != null
                && grade.getEarthScienceScore() != null
                && grade.getMusicScore() != null
                && grade.getArtScore() != null
                && grade.getPhysicalEducationScore() != null
                && grade.getTechnologyAndHomeEconomicScore() != null
                && grade.getComputerScienceScore() != null
                && grade.getSecondForeignLanguageScore() != null;
    }

    // 여러 Grade(year, semester) 조합에 대해 미리 학기별 성적 리스트 조회
    private Map<String, List<Grade>> preloadAllGrades(List<Grade> grades) {
        return grades.stream()
                .collect(Collectors.toMap(
                        g -> g.getYear() + ":" + g.getSemester(),
                        g -> gradeRepository.findAllByYearAndSemesterWithMember(g.getYear(), g.getSemester()),
                        (existing, replacement) -> existing // 충돌 시 기존 값 유지
                ));
    }

    // Grade -> GradeDto 변환
    @Override
    public GradeResponse convertToGradeDto(Grade grade, Long studentAccountId, List<Grade> allGradesForYearAndSemester) {
        Integer year = grade.getYear();
        Semester semester = grade.getSemester();
        Long targetEntranceYear = studentAccountId / 100000;
        List<Grade> sameCohortGrades = allGradesForYearAndSemester.stream()
                .filter(g -> (g.getMember().getAccountId() / 100000) == targetEntranceYear)
                .toList();
        String gradeRankForYear = calculateGradeRank(grade, sameCohortGrades);
        return GradeResponse.builder()
                .id(grade.getId())
                .studentId(grade.getMember().getId())
                .year(year)
                .semester(semester)
                .gradeRank(gradeRankForYear)
                .국어(createSubjectScore(grade.getKoreanLanguageScore(), sameCohortGrades.stream().map(Grade::getKoreanLanguageScore).toList()))
                .수학(createSubjectScore(grade.getMathematicsScore(), sameCohortGrades.stream().map(Grade::getMathematicsScore).toList()))
                .영어(createSubjectScore(grade.getEnglishScore(), sameCohortGrades.stream().map(Grade::getEnglishScore).toList()))
                .사회(createSubjectScore(grade.getSocialStudiesScore(), sameCohortGrades.stream().map(Grade::getSocialStudiesScore).toList()))
                .한국사(createSubjectScore(grade.getHistoryScore(), sameCohortGrades.stream().map(Grade::getHistoryScore).toList()))
                .윤리(createSubjectScore(grade.getEthicsScore(), sameCohortGrades.stream().map(Grade::getEthicsScore).toList()))
                .경제(createSubjectScore(grade.getEconomicsScore(), sameCohortGrades.stream().map(Grade::getEconomicsScore).toList()))
                .물리(createSubjectScore(grade.getPhysicsScore(), sameCohortGrades.stream().map(Grade::getPhysicsScore).toList()))
                .화학(createSubjectScore(grade.getChemistryScore(), sameCohortGrades.stream().map(Grade::getChemistryScore).toList()))
                .생명과학(createSubjectScore(grade.getBiologyScore(), sameCohortGrades.stream().map(Grade::getBiologyScore).toList()))
                .지구과학(createSubjectScore(grade.getEarthScienceScore(), sameCohortGrades.stream().map(Grade::getEarthScienceScore).toList()))
                .음악(createSubjectScore(grade.getMusicScore(), sameCohortGrades.stream().map(Grade::getMusicScore).toList()))
                .미술(createSubjectScore(grade.getArtScore(), sameCohortGrades.stream().map(Grade::getArtScore).toList()))
                .체육(createSubjectScore(grade.getPhysicalEducationScore(), sameCohortGrades.stream().map(Grade::getPhysicalEducationScore).toList()))
                .기술가정(createSubjectScore(grade.getTechnologyAndHomeEconomicScore(), sameCohortGrades.stream().map(Grade::getTechnologyAndHomeEconomicScore).toList()))
                .컴퓨터(createSubjectScore(grade.getComputerScienceScore(), sameCohortGrades.stream().map(Grade::getComputerScienceScore).toList()))
                .제2외국어(createSubjectScore(grade.getSecondForeignLanguageScore(), sameCohortGrades.stream().map(Grade::getSecondForeignLanguageScore).toList()))
                .build();
    }

    // GradeDto의 SubjectScore 객체 생성
    private SubjectScoreDto createSubjectScore(Double myScore, List<Double> allScores) {
        if (myScore == null) return null;
        List<Double> validScores = allScores.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.reverseOrder())
                .toList();
        double average = Math.round(validScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 100.0) / 100.0;

        String achievementLevel;
        if (myScore >= 90) achievementLevel = "A";
        else if (myScore >= 80) achievementLevel = "B";
        else if (myScore >= 70) achievementLevel = "C";
        else if (myScore >= 60) achievementLevel = "D";
        else achievementLevel = "E";

        int rank = validScores.indexOf(myScore) + 1;
        double percentile = (double) rank / validScores.size() * 100;
        int relativeRankGrade;
        if (percentile <= 4) relativeRankGrade = 1;
        else if (percentile <= 11) relativeRankGrade = 2;
        else if (percentile <= 23) relativeRankGrade = 3;
        else if (percentile <= 40) relativeRankGrade = 4;
        else if (percentile <= 60) relativeRankGrade = 5;
        else if (percentile <= 77) relativeRankGrade = 6;
        else if (percentile <= 89) relativeRankGrade = 7;
        else if (percentile <= 96) relativeRankGrade = 8;
        else relativeRankGrade = 9;

        return SubjectScoreDto.builder()
                .score(myScore)
                .average(average)
                .achievementLevel(achievementLevel)
                .relativeRankGrade(relativeRankGrade)
                .build();
    }

    // 학년 석차 계산
    private String calculateGradeRank(Grade target, List<Grade> grades) {
        // 총점으로 석차 계산한다고 가정
        List<Grade> sorted = grades.stream()
                .sorted(Comparator.comparingDouble(this::calculateTotalScore).reversed())
                .toList();
        int rank = 1;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getId().equals(target.getId())) {
                rank = i + 1;
                break;
            }
        }
        return rank + "/" + sorted.size();  // 예: 12/175
    }

    // 전과목 평균 계산
    private double calculateTotalScore(Grade g) {
        return safe(g.getKoreanLanguageScore()) + safe(g.getMathematicsScore()) + safe(g.getEnglishScore())
                + safe(g.getSocialStudiesScore()) + safe(g.getHistoryScore()) + safe(g.getEthicsScore())
                + safe(g.getEconomicsScore()) + safe(g.getPhysicsScore()) + safe(g.getChemistryScore())
                + safe(g.getBiologyScore()) + safe(g.getEarthScienceScore()) + safe(g.getMusicScore())
                + safe(g.getArtScore()) + safe(g.getPhysicalEducationScore()) + safe(g.getTechnologyAndHomeEconomicScore())
                + safe(g.getComputerScienceScore()) + safe(g.getSecondForeignLanguageScore());
    }

    // NPE 방지
    private double safe(Double d) {
        return d == null ? 0.0 : d;
    }
}
