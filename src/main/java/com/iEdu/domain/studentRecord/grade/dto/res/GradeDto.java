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
public class GradeDto {
    private Long id;
    private Long studentId;
    private Integer year;
    private Semester semester;
    private String gradeRank;
    private SubjectScore 국어;
    private SubjectScore 수학;
    private SubjectScore 영어;
    private SubjectScore 사회;
    private SubjectScore 한국사;
    private SubjectScore 윤리;
    private SubjectScore 경제;
    private SubjectScore 물리;
    private SubjectScore 화학;
    private SubjectScore 생명과학;
    private SubjectScore 지구과학;
    private SubjectScore 음악;
    private SubjectScore 미술;
    private SubjectScore 체육;
    private SubjectScore 기술가정;
    private SubjectScore 컴퓨터;
    private SubjectScore 제2외국어;
}
