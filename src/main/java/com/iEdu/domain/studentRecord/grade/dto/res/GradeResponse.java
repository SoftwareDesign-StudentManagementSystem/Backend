package com.iEdu.domain.studentRecord.grade.dto.res;

import com.iEdu.global.common.enums.Semester;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GradeResponse {
    private Long id;
    private Long studentId;
    private Integer year;
    private Semester semester;
    private String gradeRank;
    private SubjectScoreDto 국어;
    private SubjectScoreDto 수학;
    private SubjectScoreDto 영어;
    private SubjectScoreDto 사회;
    private SubjectScoreDto 한국사;
    private SubjectScoreDto 윤리;
    private SubjectScoreDto 경제;
    private SubjectScoreDto 물리;
    private SubjectScoreDto 화학;
    private SubjectScoreDto 생명과학;
    private SubjectScoreDto 지구과학;
    private SubjectScoreDto 음악;
    private SubjectScoreDto 미술;
    private SubjectScoreDto 체육;
    private SubjectScoreDto 기술가정;
    private SubjectScoreDto 컴퓨터;
    private SubjectScoreDto 제2외국어;
}
