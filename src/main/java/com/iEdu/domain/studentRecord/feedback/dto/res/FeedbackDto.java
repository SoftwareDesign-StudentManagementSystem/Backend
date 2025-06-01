package com.iEdu.domain.studentRecord.feedback.dto.res;

import com.iEdu.domain.studentRecord.feedback.entity.FeedbackCategory;
import com.iEdu.global.common.enums.Semester;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class FeedbackDto {
    private Long id;
    private Long studentId;
    private String teacherName;
    private Integer year;
    private Semester semester;
    private LocalDate date;
    private FeedbackCategory category;  // 피드백 카테고리
    private String content;             // 피드백 내용
}
