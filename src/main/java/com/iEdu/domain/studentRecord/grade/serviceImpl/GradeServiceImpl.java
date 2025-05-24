package com.iEdu.domain.studentRecord.grade.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.studentRecord.attendance.entity.Attendance;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeForm;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeUpdateForm;
import com.iEdu.domain.studentRecord.grade.dto.res.GradeDto;
import com.iEdu.domain.studentRecord.grade.dto.res.SubjectScore;
import com.iEdu.domain.studentRecord.grade.entity.Grade;
import com.iEdu.domain.studentRecord.grade.entity.GradePage;
import com.iEdu.domain.studentRecord.grade.repository.GradeQueryRepository;
import com.iEdu.domain.studentRecord.grade.repository.GradeRepository;
import com.iEdu.domain.studentRecord.grade.service.GradeService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {
    private final GradeRepository gradeRepository;
    private final MemberRepository memberRepository;
    private final GradeQueryRepository gradeQueryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 본인의 모든 성적 조회 [학생 권한]
    @Override
    @Transactional
    public Page<GradeDto> getMyAllGrade(Pageable pageable, LoginUserDto loginUser){
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"))
        );
        // ROLE_STUDENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Page<Grade> gradePage = gradeRepository.findAllByMemberId(loginUser.getId(), sortedPageable);
        return gradePage.map(grade -> convertToGradeDto(grade, loginUser.getAccountId()));
    }

    // 학생의 모든 성적 조회 [학부모/선생님 권한]
    @Override
    @Transactional
    public Page<GradeDto> getAllGrade(Long studentId, Pageable pageable, LoginUserDto loginUser){
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"))
        );
        Member student = memberRepository.getById(studentId);
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        validateAccessToStudent(loginUser, studentId);
        Page<Grade> gradePage = gradeRepository.findAllByMemberId(studentId, sortedPageable);
        return gradePage.map(grade -> convertToGradeDto(grade, student.getAccountId()));
    }

    // (학년/학기)로 본인 성적 조회 [학생 권한]
    @Override
    @Transactional
    public GradeDto getMyFilterGrade(Integer year, Integer semester, LoginUserDto loginUser){
        // ROLE_STUDENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Semester semesterEnum = convertToSemesterEnum(semester);
        Grade grade = gradeRepository
                .findByMemberIdAndYearAndSemester(loginUser.getId(), year, semesterEnum)
                .orElseThrow(() -> new ServiceException(ReturnCode.GRADE_NOT_FOUND));
        return convertToGradeDto(grade, loginUser.getAccountId());
    }

    // (학년/반/번호/학기)로 학생들 성적 조회 [선생님 권한]
    @Override
    @Transactional
    public List<GradeDto> getStudentsGrade(Integer year, Integer classId, Integer number, Integer semester, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Semester semesterEnum = convertToSemesterEnum(semester);
        List<Grade> grades = gradeQueryRepository.findAllByStudentInfoAndSemesterAndYear(
                year, classId, number, semesterEnum, year
        );
        // 학급 전체 성적 데이터 기준으로 랭크 계산
        return grades.stream()
                .map(grade -> convertToGradeDto(grade, grade.getMember().getAccountId()))
                .toList();
    }

    // (학년/학기)로 학생 성적 조회 [학부모/선생님 권한]
    @Override
    @Transactional
    public GradeDto getFilterGrade(Long studentId, Integer year, Integer semester, LoginUserDto loginUser){
        Member student = memberRepository.getById(studentId);
        // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
        validateAccessToStudent(loginUser, studentId);
        Semester semesterEnum = convertToSemesterEnum(semester);
        Grade grade = gradeRepository
                .findByMemberIdAndYearAndSemester(studentId, year, semesterEnum)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return convertToGradeDto(grade, student.getAccountId());
    }

    // 학생 성적 생성 [선생님 권한]
    @Override
    @Transactional
    public void createGrade(Long studentId, GradeForm gradeForm, LoginUserDto loginUser) {
        // ROLE_TEACHER 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Member student = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        // 선생님 담당 과목 확인
        Member.Subject subject = loginUser.getSubject();
        if (subject == null) {
            throw new ServiceException(ReturnCode.INVALID_SUBJECT);
        }
        Integer year = gradeForm.getYear();
        Semester semester = gradeForm.getSemester();
        Double score = gradeForm.getScore();
        // 기존 성적 존재 여부 확인
        Grade grade = gradeRepository.findByMemberAndYearAndSemester(student, year, semester)
                .orElseGet(() -> Grade.builder()
                        .member(student)
                        .year(year)
                        .semester(semester)
                        .build());
        // 과목별 점수 입력
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
        gradeRepository.save(grade);
        // 모든 과목 점수가 입력됐는지 확인
        if (isAllSubjectsFilled(grade)) {
            Notification notification = Notification.builder()
                    .receiverId(studentId)
                    .objectId(grade.getId())
                    .content(year + "학년 " + semester.toKoreanString() + " 성적이 등록되었습니다")
                    .targetObject(Notification.TargetObject.Grade)
                    .build();
            try {
                // 성적 생성 이벤트 생성
                String message = objectMapper.writeValueAsString(notification);
                kafkaTemplate.send("grade-topic", message);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize Notification: {}", e.getMessage());
            }
        }
    }

    // 학생 성적 수정 [선생님 권한]
    @Override
    @Transactional
    public void updateGrade(Long gradeId, GradeUpdateForm gradeUpdateForm, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ServiceException(ReturnCode.GRADE_NOT_FOUND));
        // 선생님 담당 과목 확인
        Member.Subject subject = loginUser.getSubject();
        if (subject == null) {
            throw new ServiceException(ReturnCode.INVALID_SUBJECT);
        }
        Double newScore = gradeUpdateForm.getScore();
        // 담당 과목에 맞는 필드만 업데이트
        switch (subject) {
            case 국어 -> grade.setKoreanLanguageScore(newScore);
            case 수학 -> grade.setMathematicsScore(newScore);
            case 영어 -> grade.setEnglishScore(newScore);
            case 사회 -> grade.setSocialStudiesScore(newScore);
            case 한국사 -> grade.setHistoryScore(newScore);
            case 윤리 -> grade.setEthicsScore(newScore);
            case 경제 -> grade.setEconomicsScore(newScore);
            case 물리 -> grade.setPhysicsScore(newScore);
            case 화학 -> grade.setChemistryScore(newScore);
            case 생명과학 -> grade.setBiologyScore(newScore);
            case 지구과학 -> grade.setEarthScienceScore(newScore);
            case 음악 -> grade.setMusicScore(newScore);
            case 미술 -> grade.setArtScore(newScore);
            case 체육 -> grade.setPhysicalEducationScore(newScore);
            case 기술가정 -> grade.setTechnologyAndHomeEconomicScore(newScore);
            case 컴퓨터 -> grade.setComputerScienceScore(newScore);
            case 제2외국어 -> grade.setSecondForeignLanguageScore(newScore);
            default -> throw new ServiceException(ReturnCode.INVALID_SUBJECT);
        }
        // 성적 수정 이벤트 생성
        Notification notification = Notification.builder()
                .receiverId(grade.getMember().getId())
                .objectId(grade.getId())
                .content(grade.getYear() + "학년 " + grade.getSemester().toKoreanString() + " " + subject + " 점수가 수정되었습니다")
                .targetObject(Notification.TargetObject.Grade)
                .build();
        try {
            String message = objectMapper.writeValueAsString(notification);
            kafkaTemplate.send("grade-topic", message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Notification: {}", e.getMessage());
        }
    }

    // 학생 성적 삭제 [선생님 권한]
    @Override
    @Transactional
    public void deleteGrade(Long gradeId, LoginUserDto loginUser){
        // ROLE_TEACHER 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ServiceException(ReturnCode.GRADE_NOT_FOUND));
        // 선생님 담당 과목 확인
        Member.Subject subject = loginUser.getSubject();
        if (subject == null) {
            throw new ServiceException(ReturnCode.INVALID_SUBJECT);
        }
        // 담당 과목 점수만 null로 설정 (실제 "삭제" 대신)
        switch (subject) {
            case 국어 -> grade.setKoreanLanguageScore(null);
            case 수학 -> grade.setMathematicsScore(null);
            case 영어 -> grade.setEnglishScore(null);
            case 사회 -> grade.setSocialStudiesScore(null);
            case 한국사 -> grade.setHistoryScore(null);
            case 윤리 -> grade.setEthicsScore(null);
            case 경제 -> grade.setEconomicsScore(null);
            case 물리 -> grade.setPhysicsScore(null);
            case 화학 -> grade.setChemistryScore(null);
            case 생명과학 -> grade.setBiologyScore(null);
            case 지구과학 -> grade.setEarthScienceScore(null);
            case 음악 -> grade.setMusicScore(null);
            case 미술 -> grade.setArtScore(null);
            case 체육 -> grade.setPhysicalEducationScore(null);
            case 기술가정 -> grade.setTechnologyAndHomeEconomicScore(null);
            case 컴퓨터 -> grade.setComputerScienceScore(null);
            case 제2외국어 -> grade.setSecondForeignLanguageScore(null);
            default -> throw new ServiceException(ReturnCode.INVALID_SUBJECT);
        }
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = GradePage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
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

    // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리
    private void validateAccessToStudent(LoginUserDto loginUser, Long studentId) {
        Member.MemberRole role = loginUser.getRole();
        if (role != Member.MemberRole.ROLE_PARENT && role != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        // ROLE_PARENT인 경우 자녀인지 확인
        if (role == Member.MemberRole.ROLE_PARENT) {
            Member parent = loginUser.ConvertToMember();
            boolean isMyChild = parent.getFollowList().stream()
                    .map(MemberFollow::getFollowed)
                    .anyMatch(child -> child != null && child.getId().equals(studentId));
            if (!isMyChild) {
                throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
            }
        }
    }

    // 학기 Integer -> Enum 변환
    private Semester convertToSemesterEnum(Integer semester) {
        return switch (semester) {
            case 1 -> Semester.FIRST_SEMESTER;
            case 2 -> Semester.SECOND_SEMESTER;
            default -> throw new ServiceException(ReturnCode.INVALID_SEMESTER);
        };
    }

    // Grade -> GradeDto 변환
    private GradeDto convertToGradeDto(Grade grade, Long studentAccountId) {
        Integer year = grade.getYear();
        Semester semester = grade.getSemester();

        Long targetEntranceYear = studentAccountId / 100000;

        // 해당 학년과 학기의 모든 성적 불러오기
        List<Grade> allGradesForYearAndSemester = gradeRepository.findAllByYearAndSemester(year, semester);

        // 입학 연도 필터링 → 같은 입학연도 학생들만 남김
        List<Grade> sameCohortGrades = allGradesForYearAndSemester.stream()
                .filter(g -> (g.getMember().getAccountId() / 100000) == targetEntranceYear)
                .toList();
        String gradeRankForYear = calculateGradeRank(grade, sameCohortGrades);
        return GradeDto.builder()
                .id(grade.getId())
                .studentId(grade.getMember().getId())
                .profileImageUrl(grade.getMember().getProfileImageUrl())
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
    private SubjectScore createSubjectScore(Double myScore, List<Double> allScores) {
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

        return SubjectScore.builder()
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
        return g.getKoreanLanguageScore() + g.getMathematicsScore() + g.getEnglishScore()
                + g.getSocialStudiesScore() + g.getHistoryScore() + g.getEthicsScore()
                + g.getEconomicsScore() + g.getPhysicsScore() + g.getChemistryScore()
                + g.getBiologyScore() + g.getEarthScienceScore() + g.getMusicScore()
                + g.getArtScore() + g.getPhysicalEducationScore() + g.getTechnologyAndHomeEconomicScore()
                + g.getComputerScienceScore() + g.getSecondForeignLanguageScore();
    }
}
