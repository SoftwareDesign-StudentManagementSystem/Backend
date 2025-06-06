package com.iEdu.domain.studentRecord.grade.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubjectScore {
    private Double score;         // 개인 점수
    private Double average;       // 전체 평균
    private String achievementLevel; // 성취도 (A~E)
    private Integer relativeRankGrade; // 석차 등급 (1~9등급)
}
