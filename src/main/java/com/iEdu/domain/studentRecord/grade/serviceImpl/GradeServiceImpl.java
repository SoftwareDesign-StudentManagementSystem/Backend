package com.iEdu.domain.studentRecord.grade.serviceImpl;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.grade.dto.res.GradeDto;
import com.iEdu.domain.studentRecord.grade.dto.res.SubjectScore;
import com.iEdu.domain.studentRecord.grade.entity.Grade;
import com.iEdu.domain.studentRecord.grade.entity.GradePage;
import com.iEdu.domain.studentRecord.grade.repository.GradeRepository;
import com.iEdu.domain.studentRecord.grade.service.GradeService;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;

    // 선택한 학생의 성적 조회 [학생/학부모는 본인/자녀 성적 조회]
    @Override
    @Transactional
    public Page<GradeDto> findByMemberId(Long studentId, LoginUserDto loginUser, Pageable pageable){
        checkPageSize(pageable.getPageSize());

        // 권한 확인


        Page<Grade> grades = gradeRepository.findByMemberId(studentId, pageable);
        return grades.map(this::convertToGradeDto);
    }


    // 요청 페이지 수 제한
    public void checkPageSize(int pageSize) {
        int maxPageSize = GradePage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // Grade를 GradeDto로 변환
    private GradeDto convertToGradeDto(Grade grade) {
        int studentYear = grade.getMember().getYear();  // 학년
        Grade.Semester semester = grade.getSemester();

        List<Grade> allGrades = gradeRepository.findAllByMember_YearAndSemester(studentYear, semester);

        return GradeDto.builder()
                .id(grade.getId())
                .studentId(grade.getMember().getId())
                .profileImageUrl(grade.getMember().getProfileImageUrl())
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
