package com.iEdu.domain.studentRecord.grade.serviceImpl;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeForm;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeUpdateForm;
import com.iEdu.domain.studentRecord.grade.dto.res.GradeDto;
import com.iEdu.domain.studentRecord.grade.dto.res.SubjectScore;
import com.iEdu.domain.studentRecord.grade.entity.Grade;
import com.iEdu.domain.studentRecord.grade.repository.GradeRepository;
import com.iEdu.domain.studentRecord.grade.service.GradeService;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {
    private final GradeRepository gradeRepository;
    private final MemberRepository memberRepository;

    // 본인의 모든 성적 조회 [학생 권한]
    @Override
    @Transactional
    public Page<GradeDto> getMyAllGrade(Pageable pageable, LoginUserDto loginUser){
        // ROLE_STUDENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        checkPageSize(pageable.getPageSize());
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"))
        );
        Page<Grade> gradePage = gradeRepository.findAllByMemberId(loginUser.getId(), sortedPageable);
        return gradePage.map(this::convertToGradeDto);
    }

    // 학생의 모든 성적 조회 [학부모/선생님 권한]
    @Override
    @Transactional
    public Page<GradeDto> getAllGrade(Long studentId, Pageable pageable, LoginUserDto loginUser){
        Member.MemberRole role = loginUser.getRole();
        // 정렬 조건 추가: year(내림차순), semester(SECOND_SEMESTER 우선)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("semester"))
        );
        if (role == Member.MemberRole.ROLE_TEACHER) {
            // 선생님: 학생만 조회 가능
            Page<Grade> gradePage = gradeRepository.findAllByMemberId(studentId, sortedPageable);
            return gradePage.map(this::convertToGradeDto);

        } else if (role == Member.MemberRole.ROLE_PARENT) {
            // 학부모: 본인의 followList에 있는 학생(자녀)만 조회 가능
            Member parent = loginUser.ConvertToMember();
            boolean isMyChild = parent.getFollowList().stream()
                    .map(MemberFollow::getFollowed)
                    .anyMatch(child -> child != null && child.getId().equals(studentId));
            if (!isMyChild) {
                throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
            }
            Page<Grade> gradePage = gradeRepository.findAllByMemberId(studentId, sortedPageable);
            return gradePage.map(this::convertToGradeDto);
        }
        // 권한 없음
        throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
    }

    // (학년&학기)로 본인 성적 조회 [학생 권한]
    @Override
    @Transactional
    public GradeDto getMyFilterGrade(Integer year, Integer semester, LoginUserDto loginUser){
        // ROLE_STUDENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Grade.Semester semesterEnum = (semester == 1) ? Grade.Semester.FIRST_SEMESTER : Grade.Semester.SECOND_SEMESTER;
        Grade grade = gradeRepository.findByMemberIdAndYearAndSemester(loginUser.getId(), year, semesterEnum)
                .orElseThrow(() -> new ServiceException(ReturnCode.GRADE_NOT_FOUND));
        return convertToGradeDto(grade);
    }

    // (학년&학기)로 학생 성적 조회 [학부모/선생님 권한]
    @Override
    @Transactional
    public GradeDto getFilterGrade(Long studentId, Integer year, Integer semester, LoginUserDto loginUser){
        Member.MemberRole role = loginUser.getRole();
        Grade.Semester semesterEnum = (semester == 1) ? Grade.Semester.FIRST_SEMESTER : Grade.Semester.SECOND_SEMESTER;
        if (role == Member.MemberRole.ROLE_TEACHER) {
            // 선생님: 학생만 조회 가능
            Grade grade = gradeRepository.findByMemberIdAndYearAndSemester(studentId, year, semesterEnum)
                    .orElseThrow(() -> new ServiceException(ReturnCode.GRADE_NOT_FOUND));
            return convertToGradeDto(grade);
        } else if (role == Member.MemberRole.ROLE_PARENT) {
            // 학부모: 본인의 followList에 있는 학생(자녀)만 조회 가능
            Member parent = loginUser.ConvertToMember();
            boolean isMyChild = parent.getFollowList().stream()
                    .map(MemberFollow::getFollowed)
                    .anyMatch(child -> child != null && child.getId().equals(studentId));
            if (!isMyChild) {
                throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
            }
            Grade grade = gradeRepository.findByMemberIdAndYearAndSemester(studentId, year, semesterEnum)
                    .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
            return convertToGradeDto(grade);
        }
        // 권한 없음
        throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
    }

    // 학생 성적 생성 [선생님 권한]
    @Override
    @Transactional
    public void createGrade(Long studentId, GradeForm gradeForm, LoginUserDto loginUser){
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
        Grade.Semester semester = gradeForm.getSemester();
        Double score = gradeForm.getScore();
        // 기존 성적 존재 여부 확인
        Optional<Grade> existingGradeOpt = gradeRepository.findByMemberAndYearAndSemester(student, year, semester);
        if (existingGradeOpt.isPresent()) {
            // 기존 성적 있으면 업데이트
            Grade grade = existingGradeOpt.get();
            switch (subject) {
                case KOREAN_LANGUAGE -> grade.setKoreanLanguageScore(score);
                case MATHEMATICS -> grade.setMathematicsScore(score);
                case ENGLISH -> grade.setEnglishScore(score);
                case SOCIAL_STUDIES -> grade.setSocialStudiesScore(score);
                case HISTORY -> grade.setHistoryScore(score);
                case ETHICS -> grade.setEthicsScore(score);
                case ECONOMICS -> grade.setEconomicsScore(score);
                case PHYSICS -> grade.setPhysicsScore(score);
                case CHEMISTRY -> grade.setChemistryScore(score);
                case BIOLOGY -> grade.setBiologyScore(score);
                case EARTH_SCIENCE -> grade.setEarthScienceScore(score);
                case MUSIC -> grade.setMusicScore(score);
                case ART -> grade.setArtScore(score);
                case PHYSICAL_EDUCATION -> grade.setPhysicalEducationScore(score);
                case TECHNOLOGY_AND_HOME_ECONOMICS -> grade.setTechnologyAndHomeEconomicScore(score);
                case COMPUTER_SCIENCE -> grade.setComputerScienceScore(score);
                case SECOND_FOREIGN_LANGUAGE -> grade.setSecondForeignLanguageScore(score);
                default -> throw new ServiceException(ReturnCode.INVALID_SUBJECT);
            }
            gradeRepository.save(grade);
        } else {
            // 성적이 없으면 새로 생성
            Grade.GradeBuilder gradeBuilder = Grade.builder()
                    .member(student)
                    .year(year)
                    .semester(semester);
            switch (subject) {
                case KOREAN_LANGUAGE -> gradeBuilder.koreanLanguageScore(score);
                case MATHEMATICS -> gradeBuilder.mathematicsScore(score);
                case ENGLISH -> gradeBuilder.englishScore(score);
                case SOCIAL_STUDIES -> gradeBuilder.socialStudiesScore(score);
                case HISTORY -> gradeBuilder.historyScore(score);
                case ETHICS -> gradeBuilder.ethicsScore(score);
                case ECONOMICS -> gradeBuilder.economicsScore(score);
                case PHYSICS -> gradeBuilder.physicsScore(score);
                case CHEMISTRY -> gradeBuilder.chemistryScore(score);
                case BIOLOGY -> gradeBuilder.biologyScore(score);
                case EARTH_SCIENCE -> gradeBuilder.earthScienceScore(score);
                case MUSIC -> gradeBuilder.musicScore(score);
                case ART -> gradeBuilder.artScore(score);
                case PHYSICAL_EDUCATION -> gradeBuilder.physicalEducationScore(score);
                case TECHNOLOGY_AND_HOME_ECONOMICS -> gradeBuilder.technologyAndHomeEconomicScore(score);
                case COMPUTER_SCIENCE -> gradeBuilder.computerScienceScore(score);
                case SECOND_FOREIGN_LANGUAGE -> gradeBuilder.secondForeignLanguageScore(score);
                default -> throw new ServiceException(ReturnCode.INVALID_SUBJECT);
            }
            gradeRepository.save(gradeBuilder.build());
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
            case KOREAN_LANGUAGE -> grade.setKoreanLanguageScore(newScore);
            case MATHEMATICS -> grade.setMathematicsScore(newScore);
            case ENGLISH -> grade.setEnglishScore(newScore);
            case SOCIAL_STUDIES -> grade.setSocialStudiesScore(newScore);
            case HISTORY -> grade.setHistoryScore(newScore);
            case ETHICS -> grade.setEthicsScore(newScore);
            case ECONOMICS -> grade.setEconomicsScore(newScore);
            case PHYSICS -> grade.setPhysicsScore(newScore);
            case CHEMISTRY -> grade.setChemistryScore(newScore);
            case BIOLOGY -> grade.setBiologyScore(newScore);
            case EARTH_SCIENCE -> grade.setEarthScienceScore(newScore);
            case MUSIC -> grade.setMusicScore(newScore);
            case ART -> grade.setArtScore(newScore);
            case PHYSICAL_EDUCATION -> grade.setPhysicalEducationScore(newScore);
            case TECHNOLOGY_AND_HOME_ECONOMICS -> grade.setTechnologyAndHomeEconomicScore(newScore);
            case COMPUTER_SCIENCE -> grade.setComputerScienceScore(newScore);
            case SECOND_FOREIGN_LANGUAGE -> grade.setSecondForeignLanguageScore(newScore);
            default -> throw new ServiceException(ReturnCode.INVALID_SUBJECT);
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
            case KOREAN_LANGUAGE -> grade.setKoreanLanguageScore(null);
            case MATHEMATICS -> grade.setMathematicsScore(null);
            case ENGLISH -> grade.setEnglishScore(null);
            case SOCIAL_STUDIES -> grade.setSocialStudiesScore(null);
            case HISTORY -> grade.setHistoryScore(null);
            case ETHICS -> grade.setEthicsScore(null);
            case ECONOMICS -> grade.setEconomicsScore(null);
            case PHYSICS -> grade.setPhysicsScore(null);
            case CHEMISTRY -> grade.setChemistryScore(null);
            case BIOLOGY -> grade.setBiologyScore(null);
            case EARTH_SCIENCE -> grade.setEarthScienceScore(null);
            case MUSIC -> grade.setMusicScore(null);
            case ART -> grade.setArtScore(null);
            case PHYSICAL_EDUCATION -> grade.setPhysicalEducationScore(null);
            case TECHNOLOGY_AND_HOME_ECONOMICS -> grade.setTechnologyAndHomeEconomicScore(null);
            case COMPUTER_SCIENCE -> grade.setComputerScienceScore(null);
            case SECOND_FOREIGN_LANGUAGE -> grade.setSecondForeignLanguageScore(null);
            default -> throw new ServiceException(ReturnCode.INVALID_SUBJECT);
        }
    }

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = MemberPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // Grade를 GradeDto로 변환
    private GradeDto convertToGradeDto(Grade grade) {
        Integer gradeYear = grade.getYear();  // 학년
        Grade.Semester semester = grade.getSemester();

        List<Grade> allGrades = gradeRepository.findAllByMember_YearAndSemester(gradeYear, semester);

        return GradeDto.builder()
                .id(grade.getId())
                .studentId(grade.getMember().getId())
                .profileImageUrl(grade.getMember().getProfileImageUrl())
                .year(gradeYear)
                .semester(semester)
                .koreanLanguage(createSubjectScore(grade.getKoreanLanguageScore(), allGrades.stream().map(Grade::getKoreanLanguageScore).toList()))
                .mathematics(createSubjectScore(grade.getMathematicsScore(), allGrades.stream().map(Grade::getMathematicsScore).toList()))
                .english(createSubjectScore(grade.getEnglishScore(), allGrades.stream().map(Grade::getEnglishScore).toList()))
                .socialStudies(createSubjectScore(grade.getSocialStudiesScore(), allGrades.stream().map(Grade::getSocialStudiesScore).toList()))
                .history(createSubjectScore(grade.getHistoryScore(), allGrades.stream().map(Grade::getHistoryScore).toList()))
                .ethics(createSubjectScore(grade.getEthicsScore(), allGrades.stream().map(Grade::getEthicsScore).toList()))
                .economics(createSubjectScore(grade.getEconomicsScore(), allGrades.stream().map(Grade::getEconomicsScore).toList()))
                .physics(createSubjectScore(grade.getPhysicsScore(), allGrades.stream().map(Grade::getPhysicsScore).toList()))
                .chemistry(createSubjectScore(grade.getChemistryScore(), allGrades.stream().map(Grade::getChemistryScore).toList()))
                .biology(createSubjectScore(grade.getBiologyScore(), allGrades.stream().map(Grade::getBiologyScore).toList()))
                .earthScience(createSubjectScore(grade.getEarthScienceScore(), allGrades.stream().map(Grade::getEarthScienceScore).toList()))
                .music(createSubjectScore(grade.getMusicScore(), allGrades.stream().map(Grade::getMusicScore).toList()))
                .art(createSubjectScore(grade.getArtScore(), allGrades.stream().map(Grade::getArtScore).toList()))
                .physicalEducation(createSubjectScore(grade.getPhysicalEducationScore(), allGrades.stream().map(Grade::getPhysicalEducationScore).toList()))
                .technologyAndHomeEconomics(createSubjectScore(grade.getTechnologyAndHomeEconomicScore(), allGrades.stream().map(Grade::getTechnologyAndHomeEconomicScore).toList()))
                .computerScience(createSubjectScore(grade.getComputerScienceScore(), allGrades.stream().map(Grade::getComputerScienceScore).toList()))
                .secondForeignLanguage(createSubjectScore(grade.getSecondForeignLanguageScore(), allGrades.stream().map(Grade::getSecondForeignLanguageScore).toList()))
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
}
