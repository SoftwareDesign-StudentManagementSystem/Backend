package com.iEdu.domain.studentRecord.feedback.dto.res;

import com.iEdu.domain.studentRecord.feedback.entity.FeedbackCategory;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackDto {
    private Long studentId;             // 학생 ID
    private String teacher;             // 교사 이름
    private FeedbackCategory category;  // 피드백 카테고리
    private String content;             // 피드백 내용
    private LocalDate recordedDate;     // 기록 일자
    private Boolean visibleToStudent;   // 학생 공개 여부
    private Boolean visibleToParent;    // 학부모 공개 여부
}
