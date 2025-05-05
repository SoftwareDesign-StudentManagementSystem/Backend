package com.iEdu.domain.studentRecord.feedback.dto.req;

import com.iEdu.domain.studentRecord.feedback.entity.FeedbackCategory;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackForm {
    private Long studentId;          // 학생 ID
    private Long teacherId;          // 작성 교사 ID
    private FeedbackCategory category; // 피드백 카테고리
    private String content;          // 피드백 내용
    private Boolean visibleToStudent; // 학생에게 공개 여부
    private Boolean visibleToParent;  // 학부모에게 공개 여부
    private LocalDate recordedDate;   // 기록 일자
}
